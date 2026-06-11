package study.middleware.mqreliability.notification;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationTaskRepository {
    private final JdbcTemplate jdbc;

    public NotificationTaskRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void createIfAbsent(String eventId, NotificationChannel channel, int redelivery, Instant now) {
        try {
            jdbc.update("""
                    INSERT INTO notification_task
                    (event_id,channel,status,mq_redelivery_count,next_retry_at,created_at,updated_at)
                    VALUES (?,?,'FAILED',?,?,?,?)
                    """, eventId, channel.name(), redelivery, Timestamp.from(now),
                    Timestamp.from(now), Timestamp.from(now));
        } catch (DuplicateKeyException ignored) {
            jdbc.update("""
                    UPDATE notification_task SET mq_redelivery_count=GREATEST(mq_redelivery_count,?)
                    WHERE event_id=? AND channel=?
                    """, redelivery, eventId, channel.name());
        }
    }

    public Optional<NotificationTask> claim(
            String eventId, NotificationChannel channel, String workerId, Duration timeout, Instant now
    ) {
        int changed = jdbc.update("""
                UPDATE notification_task
                SET status='PROCESSING', worker_id=?, processing_deadline=?,
                    fencing_token=fencing_token+1, updated_at=?
                WHERE event_id=? AND channel=? AND (
                    (status IN ('FAILED') AND next_retry_at <= ?)
                    OR (status='PROCESSING' AND processing_deadline < ?)
                )
                """, workerId, Timestamp.from(now.plus(timeout)), Timestamp.from(now),
                eventId, channel.name(), Timestamp.from(now), Timestamp.from(now));
        if (changed == 0) return Optional.empty();
        return find(eventId, channel);
    }

    public boolean success(NotificationTask task, Instant now) {
        return jdbc.update("""
                UPDATE notification_task SET status='SUCCESS',processing_deadline=NULL,updated_at=?
                WHERE id=? AND status='PROCESSING' AND worker_id=? AND fencing_token=?
                """, Timestamp.from(now), task.id(), task.workerId(), task.fencingToken()) == 1;
    }

    public boolean fail(NotificationTask task, Throwable error, int maxRetries, Instant now) {
        int count = task.businessRetryCount() + 1;
        String state = count >= maxRetries ? "DEAD" : "FAILED";
        long delay = Math.min(60, 1L << Math.min(count, 6));
        return jdbc.update("""
                UPDATE notification_task
                SET status=?,business_retry_count=?,next_retry_at=?,error_type=?,last_error=?,
                    processing_deadline=NULL,updated_at=?
                WHERE id=? AND status='PROCESSING' AND worker_id=? AND fencing_token=?
                """, state, count, Timestamp.from(now.plusSeconds(delay)),
                error.getClass().getSimpleName(), abbreviate(error.getMessage()), Timestamp.from(now),
                task.id(), task.workerId(), task.fencingToken()) == 1;
    }

    public Optional<NotificationTask> find(String eventId, NotificationChannel channel) {
        return jdbc.query("""
                SELECT id,event_id,channel,status,worker_id,fencing_token,business_retry_count
                FROM notification_task WHERE event_id=? AND channel=?
                """, (rs, row) -> new NotificationTask(
                rs.getLong("id"), rs.getString("event_id"),
                NotificationChannel.valueOf(rs.getString("channel")), rs.getString("status"),
                rs.getString("worker_id"), rs.getLong("fencing_token"),
                rs.getInt("business_retry_count")), eventId, channel.name()).stream().findFirst();
    }

    public int reactivate(long id, Instant now) {
        return jdbc.update("""
                UPDATE notification_task SET status='FAILED',business_retry_count=0,next_retry_at=?,
                    worker_id=NULL,processing_deadline=NULL,error_type=NULL,last_error=NULL,updated_at=?
                WHERE id=? AND status='DEAD'
                """, Timestamp.from(now), Timestamp.from(now), id);
    }

    public List<NotificationTask> findAll() {
        return jdbc.query("""
                SELECT id,event_id,channel,status,worker_id,fencing_token,business_retry_count
                FROM notification_task ORDER BY id DESC LIMIT 100
                """, (rs, row) -> new NotificationTask(
                rs.getLong("id"), rs.getString("event_id"),
                NotificationChannel.valueOf(rs.getString("channel")), rs.getString("status"),
                rs.getString("worker_id"), rs.getLong("fencing_token"),
                rs.getInt("business_retry_count")));
    }

    public int countBacklog() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification_task WHERE status <> 'SUCCESS'", Integer.class);
        return count == null ? 0 : count;
    }

    private String abbreviate(String value) {
        if (value == null) return "unknown";
        return value.substring(0, Math.min(500, value.length()));
    }
}
