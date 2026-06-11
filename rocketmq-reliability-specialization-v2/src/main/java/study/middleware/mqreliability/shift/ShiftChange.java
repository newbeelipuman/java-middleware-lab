package study.middleware.mqreliability.shift;

import java.time.Instant;

public record ShiftChange(
        String id,
        String staffCode,
        String fromShift,
        String toShift,
        String status,
        Instant createdAt
) {
}
