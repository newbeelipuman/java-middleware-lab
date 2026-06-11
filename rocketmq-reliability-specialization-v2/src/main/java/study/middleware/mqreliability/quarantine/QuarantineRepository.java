package study.middleware.mqreliability.quarantine;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class QuarantineRepository {
    private final JdbcTemplate jdbc;

    public QuarantineRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(String sourceMessageId, String eventId, String reason, String payload) {
        try {
            jdbc.update("""
                    INSERT INTO quarantined_message
                    (source_message_id,event_id,reason,payload_json,created_at)
                    VALUES (?,?,?,?,?)
                    """, sourceMessageId, eventId, reason, payload, Timestamp.from(Instant.now()));
        } catch (DuplicateKeyException ignored) {
            // Broker redelivery of the same source message must not duplicate quarantine rows.
        }
    }

    public List<QuarantineRecord> findAll() {
        return jdbc.query("""
                SELECT id,source_message_id,event_id,reason,payload_json,replay_count,created_at
                FROM quarantined_message ORDER BY id DESC LIMIT 100
                """, (rs, row) -> map(rs));
    }

    public Optional<QuarantineRecord> find(long id) {
        return jdbc.query("""
                SELECT id,source_message_id,event_id,reason,payload_json,replay_count,created_at
                FROM quarantined_message WHERE id=?
                """, (rs, row) -> map(rs), id).stream().findFirst();
    }

    public boolean markReplayed(long id, int expectedCount) {
        return jdbc.update("""
                UPDATE quarantined_message
                SET replay_count=replay_count+1,replayed_at=?
                WHERE id=? AND replay_count=?
                """, Timestamp.from(Instant.now()), id, expectedCount) == 1;
    }

    public int count() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM quarantined_message", Integer.class);
        return count == null ? 0 : count;
    }

    private QuarantineRecord map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new QuarantineRecord(rs.getLong("id"), rs.getString("source_message_id"),
                rs.getString("event_id"), rs.getString("reason"), rs.getString("payload_json"),
                rs.getInt("replay_count"), rs.getTimestamp("created_at").toInstant());
    }
}
