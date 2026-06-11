package study.middleware.redisspecialization.cache;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import study.middleware.redisspecialization.config.CacheProperties;
import study.middleware.redisspecialization.metrics.CacheMetrics;
import study.middleware.redisspecialization.schedule.ScheduleRepository;
import study.middleware.redisspecialization.schedule.StaffSchedule;

@Component
class LogicalExpireCachePolicy implements CachePolicy {

    private final ScheduleRepository repository;
    private final ScheduleCacheStore cacheStore;
    private final CacheMetrics metrics;
    private final RedissonClient redissonClient;
    private final TaskExecutor taskExecutor;
    private final CacheProperties properties;

    LogicalExpireCachePolicy(
            ScheduleRepository repository,
            ScheduleCacheStore cacheStore,
            CacheMetrics metrics,
            @Lazy RedissonClient redissonClient,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
            CacheProperties properties
    ) {
        this.repository = repository;
        this.cacheStore = cacheStore;
        this.metrics = metrics;
        this.redissonClient = redissonClient;
        this.taskExecutor = taskExecutor;
        this.properties = properties;
    }

    @Override
    public CachePolicyType type() {
        return CachePolicyType.LOGICAL_EXPIRE;
    }

    @Override
    public Optional<StaffSchedule> find(long scheduleId) {
        Optional<CachedSchedule> cached = cacheStore.get(scheduleId);
        if (cached.isEmpty()) {
            metrics.record("miss", type().name());
            return loadInitial(scheduleId);
        }
        CachedSchedule entry = cached.get();
        if (entry.missing()) {
            metrics.record("missing_hit", type().name());
            return Optional.empty();
        }
        Instant now = Instant.now();
        if (!entry.logicallyExpired(now)) {
            metrics.record("hit", type().name());
            return Optional.of(entry.value());
        }
        if (entry.logicalExpiresAt().plus(properties.logicalStaleLimit()).isBefore(now)) {
            metrics.record("stale_limit_exceeded", type().name());
            return loadInitial(scheduleId);
        }
        metrics.record("stale_hit", type().name());
        rebuildAsync(scheduleId);
        return Optional.of(entry.value());
    }

    private Optional<StaffSchedule> loadInitial(long scheduleId) {
        Optional<StaffSchedule> loaded = repository.findById(scheduleId);
        loaded.ifPresentOrElse(cacheStore::putLogical, () -> cacheStore.putMissing(scheduleId));
        return loaded;
    }

    private void rebuildAsync(long scheduleId) {
        taskExecutor.execute(() -> {
            RLock lock = redissonClient.getLock("middleware:v2:rebuild:schedule:" + scheduleId);
            boolean acquired = false;
            try {
                if (properties.lockLease().isZero()) {
                    acquired = lock.tryLock();
                } else {
                    acquired = lock.tryLock(0, properties.lockLease().toMillis(), TimeUnit.MILLISECONDS);
                }
                if (!acquired) {
                    metrics.record("rebuild_coalesced", type().name());
                    return;
                }
                Optional<StaffSchedule> loaded = repository.findById(scheduleId);
                loaded.ifPresentOrElse(cacheStore::putLogical, () -> cacheStore.putMissing(scheduleId));
                metrics.record("rebuild_success", type().name());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                metrics.record("rebuild_interrupted", type().name());
            } catch (RuntimeException exception) {
                metrics.record("rebuild_failed", type().name());
            } finally {
                if (acquired && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        });
    }
}
