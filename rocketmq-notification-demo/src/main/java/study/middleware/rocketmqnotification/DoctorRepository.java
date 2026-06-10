package study.middleware.rocketmqnotification;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class DoctorRepository {

    private static final RowMapper<Doctor> DOCTOR_ROW_MAPPER = (rs, rowNum) -> new Doctor(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("department"),
            rs.getString("specialty"),
            rs.getBoolean("available"),
            rs.getTimestamp("updated_at").toInstant()
    );

    private final JdbcTemplate jdbcTemplate;

    public DoctorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Doctor saveNew(Doctor doctor) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO doctors (name, department, specialty, available, updated_at)
                            VALUES (?, ?, ?, ?, ?)
                            """,
                    new String[]{"id"}
            );
            statement.setString(1, doctor.name());
            statement.setString(2, doctor.department());
            statement.setString(3, doctor.specialty());
            statement.setBoolean(4, doctor.available());
            statement.setTimestamp(5, Timestamp.from(doctor.updatedAt()));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("MySQL did not return a doctor id");
        }
        return new Doctor(key.longValue(), doctor.name(), doctor.department(), doctor.specialty(), doctor.available(), doctor.updatedAt());
    }

    public List<Doctor> findAll() {
        return jdbcTemplate.query(
                """
                        SELECT id, name, department, specialty, available, updated_at
                        FROM doctors
                        ORDER BY id ASC
                        """,
                DOCTOR_ROW_MAPPER
        );
    }

    public int count() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM doctors", Integer.class);
        return count == null ? 0 : count;
    }
}
