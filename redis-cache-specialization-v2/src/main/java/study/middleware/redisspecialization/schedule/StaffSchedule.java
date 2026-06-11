package study.middleware.redisspecialization.schedule;

import java.time.Instant;
import java.time.LocalDate;

public record StaffSchedule(
        long id,
        String staffCode,
        String department,
        LocalDate shiftDate,
        ShiftType shiftType,
        long version,
        Instant updatedAt
) {
}
