package study.middleware.redisspecialization.invalidation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import study.middleware.redisspecialization.cache.ScheduleCacheStore;
import study.middleware.redisspecialization.config.InvalidationProperties;
import study.middleware.redisspecialization.metrics.CacheMetrics;

class InvalidationTaskProcessorTest {

    @Test
    void deletesCacheAndMarksTaskSuccessful() {
        InvalidationTaskRepository repository = mock(InvalidationTaskRepository.class);
        ScheduleCacheStore cacheStore = mock(ScheduleCacheStore.class);
        CacheMetrics metrics = mock(CacheMetrics.class);
        InvalidationTask task = new InvalidationTask(7, "schedule:1", 0);
        when(repository.claim(7)).thenReturn(task);
        InvalidationTaskProcessor processor = new InvalidationTaskProcessor(
                repository,
                cacheStore,
                new InvalidationProperties(10, 3),
                metrics
        );

        processor.process(7);

        verify(cacheStore).delete("schedule:1");
        verify(repository).markSuccess(7);
        verify(metrics).recordInvalidation("success");
    }
}
