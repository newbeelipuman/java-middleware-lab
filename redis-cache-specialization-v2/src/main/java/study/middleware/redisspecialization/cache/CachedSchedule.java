package study.middleware.redisspecialization.cache;

import java.time.Instant;

import study.middleware.redisspecialization.schedule.StaffSchedule;

public record CachedSchedule(
        StaffSchedule value,
        boolean missing,
        Instant cachedAt,
        Instant logicalExpiresAt
) {

    public boolean logicallyExpired(Instant now) {
        return logicalExpiresAt != null && !logicalExpiresAt.isAfter(now);
    }
}
