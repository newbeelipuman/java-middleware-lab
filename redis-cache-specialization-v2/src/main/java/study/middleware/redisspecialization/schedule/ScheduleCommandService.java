package study.middleware.redisspecialization.schedule;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import study.middleware.redisspecialization.cache.ScheduleCacheStore;
import study.middleware.redisspecialization.invalidation.InvalidationTaskRepository;

@Service
public class ScheduleCommandService {

    private final ScheduleRepository scheduleRepository;
    private final InvalidationTaskRepository invalidationRepository;
    private final ScheduleCacheStore cacheStore;

    public ScheduleCommandService(
            ScheduleRepository scheduleRepository,
            InvalidationTaskRepository invalidationRepository,
            ScheduleCacheStore cacheStore
    ) {
        this.scheduleRepository = scheduleRepository;
        this.invalidationRepository = invalidationRepository;
        this.cacheStore = cacheStore;
    }

    @Transactional
    public ScheduleUpdateResult update(long scheduleId, UpdateScheduleRequest request) {
        StaffSchedule updated = scheduleRepository.update(scheduleId, request);
        long taskId = invalidationRepository.create(cacheStore.key(scheduleId));
        return new ScheduleUpdateResult(updated, taskId);
    }
}
