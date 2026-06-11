package study.middleware.redisspecialization.cache;

import java.util.Optional;

import study.middleware.redisspecialization.schedule.StaffSchedule;

public interface CachePolicy {

    CachePolicyType type();

    Optional<StaffSchedule> find(long scheduleId);
}
