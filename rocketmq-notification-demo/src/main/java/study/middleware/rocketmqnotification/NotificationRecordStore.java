package study.middleware.rocketmqnotification;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.sql.Timestamp;

@Repository
public class NotificationRecordStore {

    private static final RowMapper<NotificationRecord> NOTIFICATION_RECORD_ROW_MAPPER = (rs, rowNum) -> new NotificationRecord(
            rs.getString("event_id"),
            rs.getLong("appointment_id"),
            rs.getLong("patient_id"),
            NotificationStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("handled_at").toInstant()
    );

    private final JdbcTemplate jdbcTemplate;

    public NotificationRecordStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean saveIfAbsent(NotificationRecord record) {
        try {
            jdbcTemplate.update(
                    """
                            INSERT INTO notification_records (event_id, appointment_id, patient_id, status, handled_at)
                            VALUES (?, ?, ?, ?, ?)
                            """,
                    record.eventId(),
                    record.appointmentId(),
                    record.patientId(),
                    record.status().name(),
                    Timestamp.from(record.handledAt())
            );
            return true;
        } catch (DuplicateKeyException ignored) {
            return false;
        }
    }

    public Optional<NotificationRecord> findByEventId(String eventId) {
        return jdbcTemplate.query(
                        """
                                SELECT event_id, appointment_id, patient_id, status, handled_at
                                FROM notification_records
                                WHERE event_id = ?
                                """,
                        NOTIFICATION_RECORD_ROW_MAPPER,
                        eventId
                )
                .stream()
                .findFirst();
    }

    public List<NotificationRecord> findByAppointmentId(long appointmentId) {
        return jdbcTemplate.query(
                """
                        SELECT event_id, appointment_id, patient_id, status, handled_at
                        FROM notification_records
                        WHERE appointment_id = ?
                        ORDER BY handled_at ASC
                        """,
                NOTIFICATION_RECORD_ROW_MAPPER,
                appointmentId
        );
    }
}
