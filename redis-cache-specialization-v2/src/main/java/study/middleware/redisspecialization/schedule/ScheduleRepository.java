package study.middleware.redisspecialization.schedule;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import study.middleware.redisspecialization.metrics.CacheMetrics;

@Repository
public class ScheduleRepository {

    private final JdbcTemplate jdbcTemplate;
    private final CacheMetrics metrics;

    public ScheduleRepository(JdbcTemplate jdbcTemplate, CacheMetrics metrics) {
        this.jdbcTemplate = jdbcTemplate;
        this.metrics = metrics;
    }

    public Optional<StaffSchedule> findById(long id) {
        metrics.recordDatabaseLoad();
        return jdbcTemplate.query(
                """
                SELECT id, staff_code, department, shift_date, shift_type, version, updated_at
                FROM staff_schedule
                WHERE id = ?
                """,
                this::map,
                id
        ).stream().findFirst();
    }

    public StaffSchedule update(long id, UpdateScheduleRequest request) {
        int updated = jdbcTemplate.update(
                """
                UPDATE staff_schedule
                SET department = ?, shift_type = ?, version = version + 1
                WHERE id = ?
                """,
                request.department(), request.shiftType().name(), id
        );
        if (updated == 0) {
            throw new ScheduleNotFoundException(id);
        }
        return findById(id).orElseThrow(() -> new ScheduleNotFoundException(id));
    }

    private StaffSchedule map(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StaffSchedule(
                resultSet.getLong("id"),
                resultSet.getString("staff_code"),
                resultSet.getString("department"),
                resultSet.getDate("shift_date").toLocalDate(),
                ShiftType.valueOf(resultSet.getString("shift_type")),
                resultSet.getLong("version"),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }
}
