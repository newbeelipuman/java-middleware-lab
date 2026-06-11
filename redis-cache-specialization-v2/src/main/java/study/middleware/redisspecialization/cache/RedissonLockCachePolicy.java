package study.middleware.redisspecialization.cache;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import study.middleware.redisspecialization.config.CacheProperties;
import study.middleware.redisspecialization.metrics.CacheMetrics;
import study.middleware.redisspecialization.schedule.ScheduleRepository;
import study.middleware.redisspecialization.schedule.StaffSchedule;

@Component
class RedissonLockCachePolicy extends CacheAsideSupport {

    private final RedissonClient redissonClient;
    private final CacheProperties properties;

    RedissonLockCachePolicy(
            ScheduleRepository repository,
            ScheduleCacheStore cacheStore,
            CacheMetrics metrics,
            @Lazy RedissonClient redissonClient,
            CacheProperties properties
    ) {
        super(repository, cacheStore, metrics);
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    @Override
    public CachePolicyType type() {
        return CachePolicyType.REDISSON_LOCK;
    }

    @Override
    public Optional<StaffSchedule> find(long scheduleId) {
        Optional<StaffSchedule> cached = readCache(scheduleId);
        if (cached != null) {
            return cached;
        }
        RLock lock = redissonClient.getLock("middleware:v2:lock:schedule:" + scheduleId);
        long started = System.nanoTime();
        boolean acquired = false;
        try {
            if (properties.lockLease().isZero()) {
                acquired = lock.tryLock(properties.lockWait().toMillis(), TimeUnit.MILLISECONDS);
            } else {
                acquired = lock.tryLock(
                        properties.lockWait().toMillis(),
                        properties.lockLease().toMillis(),
                        TimeUnit.MILLISECONDS
                );
            }
            metrics.recordLockWait(type().name(), System.nanoTime() - started);
            if (!acquired) {
                metrics.record("lock_timeout", type().name());
                throw new CacheAccessException("lock timeout", new IllegalStateException("lock not acquired"));
            }
            Optional<StaffSchedule> doubleChecked = readCache(scheduleId);
            return doubleChecked != null ? doubleChecked : loadAndCache(scheduleId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CacheAccessException("lock", exception);
        } catch (RuntimeException exception) {
            if (exception instanceof CacheAccessException cacheAccessException) {
                throw cacheAccessException;
            }
            throw new CacheAccessException("lock", exception);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
