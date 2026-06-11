package study.middleware.mqreliability.outbox;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import study.middleware.mqreliability.event.DomainEventEnvelope;

@Repository
public class OutboxRepository {
    private final JdbcTemplate jdbc;

    public OutboxRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(DomainEventEnvelope<?> event, String json) {
        jdbc.update("""
                INSERT INTO outbox_event
                (event_id, event_type, aggregate_type, aggregate_id, schema_version, trace_id,
                 payload_json, status, next_retry_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'NEW', ?, ?)
                """, event.eventId(), event.eventType(), event.aggregateType(), event.aggregateId(),
                event.schemaVersion(), event.traceId(), json, Timestamp.from(event.occurredAt()),
                Timestamp.from(event.occurredAt()));
    }

    @Transactional
    public List<OutboxEvent> claim(String workerId, int limit, Duration timeout, Instant now) {
        List<Long> ids = jdbc.query("""
                SELECT id FROM outbox_event
                WHERE (status IN ('NEW','FAILED') AND next_retry_at <= ?)
                   OR (status = 'PROCESSING' AND processing_deadline < ?)
                ORDER BY id LIMIT ? FOR UPDATE SKIP LOCKED
                """, (rs, row) -> rs.getLong(1), Timestamp.from(now), Timestamp.from(now), limit);
        if (ids.isEmpty()) {
            return List.of();
        }
        Instant deadline = now.plus(timeout);
        for (Long id : ids) {
            jdbc.update("""
                    UPDATE outbox_event
                    SET status='PROCESSING', worker_id=?, processing_deadline=?,
                        fencing_token=fencing_token+1
                    WHERE id=?
                    """, workerId, Timestamp.from(deadline), id);
        }
        String placeholders = String.join(",", ids.stream().map(ignored -> "?").toList());
        return jdbc.query("""
                SELECT id,event_id,payload_json,status,worker_id,fencing_token,retry_count,next_retry_at
                FROM outbox_event WHERE id IN (%s) ORDER BY id
                """.formatted(placeholders), (rs, row) -> new OutboxEvent(
                rs.getLong("id"), rs.getString("event_id"), rs.getString("payload_json"),
                OutboxStatus.valueOf(rs.getString("status")), rs.getString("worker_id"),
                rs.getLong("fencing_token"), rs.getInt("retry_count"),
                rs.getTimestamp("next_retry_at").toInstant()), ids.toArray());
    }

    public boolean markSent(OutboxEvent event, Instant now) {
        return jdbc.update("""
                UPDATE outbox_event SET status='SENT', sent_at=?, processing_deadline=NULL
                WHERE id=? AND status='PROCESSING' AND worker_id=? AND fencing_token=?
                """, Timestamp.from(now), event.id(), event.workerId(), event.fencingToken()) == 1;
    }

    public boolean markFailed(OutboxEvent event, Throwable error, int maxRetries, Instant now) {
        int nextCount = event.retryCount() + 1;
        String status = nextCount >= maxRetries ? "DEAD" : "FAILED";
        long delaySeconds = Math.min(60, 1L << Math.min(nextCount, 6));
        return jdbc.update("""
                UPDATE outbox_event
                SET status=?, retry_count=?, next_retry_at=?, last_error=?, processing_deadline=NULL
                WHERE id=? AND status='PROCESSING' AND worker_id=? AND fencing_token=?
                """, status, nextCount, Timestamp.from(now.plusSeconds(delaySeconds)),
                abbreviate(error.getMessage()), event.id(), event.workerId(), event.fencingToken()) == 1;
    }

    public int reactivate(long id, Instant now) {
        return jdbc.update("""
                UPDATE outbox_event SET status='NEW', retry_count=0, next_retry_at=?,
                worker_id=NULL, processing_deadline=NULL, last_error=NULL
                WHERE id=? AND status='DEAD'
                """, Timestamp.from(now), id);
    }

    public List<OutboxEvent> findAll() {
        return jdbc.query("""
                SELECT id,event_id,payload_json,status,worker_id,fencing_token,retry_count,next_retry_at
                FROM outbox_event ORDER BY id DESC LIMIT 100
                """, (rs, row) -> new OutboxEvent(
                rs.getLong("id"), rs.getString("event_id"), rs.getString("payload_json"),
                OutboxStatus.valueOf(rs.getString("status")), rs.getString("worker_id"),
                rs.getLong("fencing_token"), rs.getInt("retry_count"),
                rs.getTimestamp("next_retry_at").toInstant()));
    }

    public int countBacklog() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE status <> 'SENT'", Integer.class);
        return count == null ? 0 : count;
    }

    private String abbreviate(String value) {
        if (value == null) return "unknown";
        return value.substring(0, Math.min(500, value.length()));
    }
}
