package study.middleware.redisspecialization.cache;

import java.util.Optional;

import study.middleware.redisspecialization.metrics.CacheMetrics;
import study.middleware.redisspecialization.schedule.ScheduleRepository;
import study.middleware.redisspecialization.schedule.StaffSchedule;

abstract class CacheAsideSupport implements CachePolicy {

    protected final ScheduleRepository repository;
    protected final ScheduleCacheStore cacheStore;
    protected final CacheMetrics metrics;

    CacheAsideSupport(
            ScheduleRepository repository,
            ScheduleCacheStore cacheStore,
            CacheMetrics metrics
    ) {
        this.repository = repository;
        this.cacheStore = cacheStore;
        this.metrics = metrics;
    }

    protected Optional<StaffSchedule> readCache(long scheduleId) {
        Optional<CachedSchedule> cached = cacheStore.get(scheduleId);
        if (cached.isEmpty()) {
            metrics.record("miss", type().name());
            return null;
        }
        if (cached.get().missing()) {
            metrics.record("missing_hit", type().name());
            return Optional.empty();
        }
        metrics.record("hit", type().name());
        return Optional.of(cached.get().value());
    }

    protected Optional<StaffSchedule> loadAndCache(long scheduleId) {
        Optional<StaffSchedule> loaded = repository.findById(scheduleId);
        loaded.ifPresentOrElse(cacheStore::put, () -> cacheStore.putMissing(scheduleId));
        metrics.record("database_load", type().name());
        return loaded;
    }
}
