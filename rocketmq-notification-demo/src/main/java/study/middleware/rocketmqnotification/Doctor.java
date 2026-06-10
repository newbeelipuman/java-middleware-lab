package study.middleware.rocketmqnotification;

import java.time.Instant;

public record Doctor(
        Long id,
        String name,
        String department,
        String specialty,
        boolean available,
        Instant updatedAt
) {
}
