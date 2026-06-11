package study.middleware.redisspecialization.schedule;

import java.util.Optional;
import java.util.concurrent.Semaphore;

import org.springframework.stereotype.Service;

import study.middleware.redisspecialization.cache.CacheAccessException;
import study.middleware.redisspecialization.cache.CachePolicyRegistry;
import study.middleware.redisspecialization.cache.CachePolicyType;
import study.middleware.redisspecialization.config.CacheProperties;
import study.middleware.redisspecialization.metrics.CacheMetrics;

@Service
public class ScheduleQueryService {

    private final CachePolicyRegistry policies;
    private final ScheduleRepository repository;
    private final CacheMetrics metrics;
    private final Semaphore fallbackPermits;

    public ScheduleQueryService(
            CachePolicyRegistry policies,
            ScheduleRepository repository,
            CacheMetrics metrics,
            CacheProperties properties
    ) {
        this.policies = policies;
        this.repository = repository;
        this.metrics = metrics;
        this.fallbackPermits = new Semaphore(properties.databaseFallbackPermits());
    }

    public StaffSchedule find(long scheduleId, CachePolicyType policyType) {
        try {
            return policies.get(policyType).find(scheduleId)
                    .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
        } catch (CacheAccessException exception) {
            return degradedDatabaseRead(scheduleId);
        }
    }

    private StaffSchedule degradedDatabaseRead(long scheduleId) {
        if (!fallbackPermits.tryAcquire()) {
            metrics.recordDegraded("rejected");
            throw new ScheduleTemporarilyUnavailableException(scheduleId);
        }
        try {
            metrics.recordDegraded("database_fallback");
            Optional<StaffSchedule> loaded = repository.findById(scheduleId);
            return loaded.orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
        } finally {
            fallbackPermits.release();
        }
    }
}
