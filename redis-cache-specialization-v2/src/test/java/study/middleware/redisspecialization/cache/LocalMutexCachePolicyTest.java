package study.middleware.redisspecialization.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import study.middleware.redisspecialization.metrics.CacheMetrics;
import study.middleware.redisspecialization.schedule.ScheduleRepository;
import study.middleware.redisspecialization.schedule.ShiftType;
import study.middleware.redisspecialization.schedule.StaffSchedule;

class LocalMutexCachePolicyTest {

    @Test
    void concurrentMissesLoadDatabaseOnce() throws Exception {
        ScheduleRepository repository = mock(ScheduleRepository.class);
        ScheduleCacheStore cacheStore = mock(ScheduleCacheStore.class);
        CacheMetrics metrics = mock(CacheMetrics.class);
        StaffSchedule schedule = schedule(1, 0);
        AtomicReference<CachedSchedule> cache = new AtomicReference<>();
        AtomicInteger loads = new AtomicInteger();

        when(cacheStore.get(1)).thenAnswer(ignored -> Optional.ofNullable(cache.get()));
        doAnswer(invocation -> {
            StaffSchedule value = invocation.getArgument(0);
            cache.set(new CachedSchedule(value, false, Instant.now(), null));
            return null;
        }).when(cacheStore).put(any(StaffSchedule.class));
        when(repository.findById(1)).thenAnswer(ignored -> {
            loads.incrementAndGet();
            Thread.sleep(50);
            return Optional.of(schedule);
        });

        LocalMutexCachePolicy policy = new LocalMutexCachePolicy(repository, cacheStore, metrics);
        int threads = 12;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int index = 0; index < threads; index++) {
            executor.execute(() -> {
                try {
                    start.await();
                    assertThat(policy.find(1)).contains(schedule);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();
        assertThat(loads).hasValue(1);
    }

    private StaffSchedule schedule(long id, long version) {
        return new StaffSchedule(
                id,
                "EMP-1001",
                "Emergency",
                LocalDate.of(2026, 6, 15),
                ShiftType.DAY,
                version,
                Instant.parse("2026-06-11T00:00:00Z")
        );
    }
}
