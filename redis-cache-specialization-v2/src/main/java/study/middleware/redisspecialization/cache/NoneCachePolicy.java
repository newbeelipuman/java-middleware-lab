package study.middleware.redisspecialization.cache;

import java.util.Optional;

import org.springframework.stereotype.Component;

import study.middleware.redisspecialization.metrics.CacheMetrics;
import study.middleware.redisspecialization.schedule.ScheduleRepository;
import study.middleware.redisspecialization.schedule.StaffSchedule;

@Component
class NoneCachePolicy implements CachePolicy {

    private final ScheduleRepository repository;
    private final CacheMetrics metrics;

    NoneCachePolicy(ScheduleRepository repository, CacheMetrics metrics) {
        this.repository = repository;
        this.metrics = metrics;
    }

    @Override
    public CachePolicyType type() {
        return CachePolicyType.NONE;
    }

    @Override
    public Optional<StaffSchedule> find(long scheduleId) {
        metrics.record("bypass", type().name());
        return repository.findById(scheduleId);
    }
}
