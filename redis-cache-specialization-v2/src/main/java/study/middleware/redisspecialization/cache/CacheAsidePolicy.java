package study.middleware.redisspecialization.cache;

import java.util.Optional;

import org.springframework.stereotype.Component;

import study.middleware.redisspecialization.metrics.CacheMetrics;
import study.middleware.redisspecialization.schedule.ScheduleRepository;
import study.middleware.redisspecialization.schedule.StaffSchedule;

@Component
class CacheAsidePolicy extends CacheAsideSupport {

    CacheAsidePolicy(ScheduleRepository repository, ScheduleCacheStore cacheStore, CacheMetrics metrics) {
        super(repository, cacheStore, metrics);
    }

    @Override
    public CachePolicyType type() {
        return CachePolicyType.CACHE_ASIDE;
    }

    @Override
    public Optional<StaffSchedule> find(long scheduleId) {
        Optional<StaffSchedule> cached = readCache(scheduleId);
        return cached != null ? cached : loadAndCache(scheduleId);
    }
}
