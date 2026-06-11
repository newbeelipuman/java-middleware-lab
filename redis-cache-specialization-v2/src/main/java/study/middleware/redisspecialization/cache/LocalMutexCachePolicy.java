package study.middleware.redisspecialization.cache;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

import study.middleware.redisspecialization.metrics.CacheMetrics;
import study.middleware.redisspecialization.schedule.ScheduleRepository;
import study.middleware.redisspecialization.schedule.StaffSchedule;

@Component
class LocalMutexCachePolicy extends CacheAsideSupport {

    private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    LocalMutexCachePolicy(ScheduleRepository repository, ScheduleCacheStore cacheStore, CacheMetrics metrics) {
        super(repository, cacheStore, metrics);
    }

    @Override
    public CachePolicyType type() {
        return CachePolicyType.LOCAL_MUTEX;
    }

    @Override
    public Optional<StaffSchedule> find(long scheduleId) {
        Optional<StaffSchedule> cached = readCache(scheduleId);
        if (cached != null) {
            return cached;
        }
        ReentrantLock lock = locks.computeIfAbsent(scheduleId, ignored -> new ReentrantLock());
        long started = System.nanoTime();
        lock.lock();
        metrics.recordLockWait(type().name(), System.nanoTime() - started);
        try {
            Optional<StaffSchedule> doubleChecked = readCache(scheduleId);
            return doubleChecked != null ? doubleChecked : loadAndCache(scheduleId);
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                locks.remove(scheduleId, lock);
            }
        }
    }
}
