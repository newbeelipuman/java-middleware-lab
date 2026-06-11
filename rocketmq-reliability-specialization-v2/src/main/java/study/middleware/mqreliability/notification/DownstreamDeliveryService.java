package study.middleware.mqreliability.notification;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import study.middleware.mqreliability.event.ShiftChangePayload;

@Service
public class DownstreamDeliveryService {
    private final JdbcTemplate jdbc;

    public DownstreamDeliveryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void deliver(String eventId, NotificationChannel channel, ShiftChangePayload payload) {
        if ("POISON".equals(payload.staffCode())) {
            throw new IllegalArgumentException("Poison message drill");
        }
        String key = eventId + ":" + channel.name();
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM downstream_delivery WHERE idempotency_key=?",
                Integer.class, key);
        if (existing != null && existing > 0) return;
        try {
            jdbc.update("""
                    INSERT INTO downstream_delivery (idempotency_key,result_code,delivered_at)
                    VALUES (?,'DELIVERED',?)
                    """, key, Timestamp.from(Instant.now()));
        } catch (DuplicateKeyException ignored) {
            return;
        }
        if ("TIMEOUT_ONCE".equals(payload.staffCode()) && channel == NotificationChannel.EMAIL) {
            throw new DownstreamResultUnknownException("Downstream committed before client timeout");
        }
    }
}
