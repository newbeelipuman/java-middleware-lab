package study.middleware.rocketmqnotification;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Optional;

@Repository
public class AppointmentRepository {

    private static final RowMapper<Appointment> APPOINTMENT_ROW_MAPPER = (rs, rowNum) -> new Appointment(
            rs.getLong("id"),
            rs.getLong("patient_id"),
            rs.getLong("doctor_id"),
            rs.getTimestamp("appointment_time").toInstant(),
            AppointmentStatus.valueOf(rs.getString("status"))
    );

    private final JdbcTemplate jdbcTemplate;

    public AppointmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Appointment saveNew(CreateAppointmentRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO appointments (patient_id, doctor_id, appointment_time, status)
                            VALUES (?, ?, ?, ?)
                            """,
                    new String[]{"id"}
            );
            statement.setLong(1, request.patientId());
            statement.setLong(2, request.doctorId());
            statement.setTimestamp(3, Timestamp.from(request.appointmentTime()));
            statement.setString(4, AppointmentStatus.CREATED.name());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("MySQL did not return an appointment id");
        }

        Appointment appointment = new Appointment(
                key.longValue(),
                request.patientId(),
                request.doctorId(),
                request.appointmentTime(),
                AppointmentStatus.CREATED
        );
        return appointment;
    }

    public Optional<Appointment> find(long id) {
        return jdbcTemplate.query(
                        "SELECT id, patient_id, doctor_id, appointment_time, status FROM appointments WHERE id = ?",
                        APPOINTMENT_ROW_MAPPER,
                        id
                )
                .stream()
                .findFirst();
    }
}
