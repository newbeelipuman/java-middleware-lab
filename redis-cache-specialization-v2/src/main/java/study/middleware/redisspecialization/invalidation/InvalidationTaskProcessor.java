package study.middleware.redisspecialization.invalidation;

import org.springframework.stereotype.Service;

import study.middleware.redisspecialization.cache.ScheduleCacheStore;
import study.middleware.redisspecialization.config.InvalidationProperties;
import study.middleware.redisspecialization.metrics.CacheMetrics;

@Service
public class InvalidationTaskProcessor {

    private final InvalidationTaskRepository repository;
    private final ScheduleCacheStore cacheStore;
    private final InvalidationProperties properties;
    private final CacheMetrics metrics;

    public InvalidationTaskProcessor(
            InvalidationTaskRepository repository,
            ScheduleCacheStore cacheStore,
            InvalidationProperties properties,
            CacheMetrics metrics
    ) {
        this.repository = repository;
        this.cacheStore = cacheStore;
        this.properties = properties;
        this.metrics = metrics;
    }

    public void processDue() {
        repository.findDueIds(properties.batchSize()).forEach(this::process);
    }

    public void process(long taskId) {
        InvalidationTask task = repository.claim(taskId);
        if (task == null) {
            return;
        }
        try {
            cacheStore.delete(task.cacheKey());
            repository.markSuccess(task.id());
            metrics.recordInvalidation("success");
        } catch (RuntimeException exception) {
            repository.markFailure(task, exception, properties.maxRetries());
            metrics.recordInvalidation("failed");
        }
    }
}
