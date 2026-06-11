package study.middleware.mqreliability.transaction;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TransactionFactRepository {
    private final JdbcTemplate jdbc;

    public TransactionFactRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void committed(String eventId, String aggregateId) {
        jdbc.update("""
                INSERT INTO transaction_event_fact
                (event_id,aggregate_id,transaction_state,updated_at)
                VALUES (?,?,'COMMITTED',?)
                """, eventId, aggregateId, Timestamp.from(Instant.now()));
    }

    public Optional<String> state(String eventId) {
        return jdbc.query("SELECT transaction_state FROM transaction_event_fact WHERE event_id=?",
                (rs, row) -> rs.getString(1), eventId).stream().findFirst();
    }
}
