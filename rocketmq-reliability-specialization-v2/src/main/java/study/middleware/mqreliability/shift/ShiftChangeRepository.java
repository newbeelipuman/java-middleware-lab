package study.middleware.mqreliability.shift;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ShiftChangeRepository {
    private final JdbcTemplate jdbc;

    public ShiftChangeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ShiftChange insert(CreateShiftChangeRequest request, Instant now) {
        return insert(UUID.randomUUID().toString(), request, now);
    }

    public ShiftChange insert(String id, CreateShiftChangeRequest request, Instant now) {
        jdbc.update("""
                INSERT INTO shift_change_request
                (id, staff_code, from_shift, to_shift, status, created_at)
                VALUES (?, ?, ?, ?, 'REQUESTED', ?)
                """, id, request.staffCode(), request.fromShift(), request.toShift(), Timestamp.from(now));
        return new ShiftChange(id, request.staffCode(), request.fromShift(),
                request.toShift(), "REQUESTED", now);
    }

    public Optional<ShiftChange> find(String id) {
        return jdbc.query("""
                SELECT id, staff_code, from_shift, to_shift, status, created_at
                FROM shift_change_request WHERE id = ?
                """, (rs, row) -> new ShiftChange(
                rs.getString("id"), rs.getString("staff_code"), rs.getString("from_shift"),
                rs.getString("to_shift"), rs.getString("status"),
                rs.getTimestamp("created_at").toInstant()), id).stream().findFirst();
    }
}
