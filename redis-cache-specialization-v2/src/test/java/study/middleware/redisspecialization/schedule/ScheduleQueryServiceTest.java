package study.middleware.redisspecialization.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import study.middleware.redisspecialization.cache.CacheAccessException;
import study.middleware.redisspecialization.cache.CachePolicy;
import study.middleware.redisspecialization.cache.CachePolicyRegistry;
import study.middleware.redisspecialization.cache.CachePolicyType;
import study.middleware.redisspecialization.config.CacheProperties;
import study.middleware.redisspecialization.metrics.CacheMetrics;

class ScheduleQueryServiceTest {

    @Test
    void fallsBackToDatabaseWhenRedisIsUnavailable() {
        CachePolicy policy = mock(CachePolicy.class);
        CachePolicyRegistry registry = mock(CachePolicyRegistry.class);
        ScheduleRepository repository = mock(ScheduleRepository.class);
        CacheMetrics metrics = mock(CacheMetrics.class);
        StaffSchedule schedule = schedule();
        when(registry.get(CachePolicyType.CACHE_ASIDE)).thenReturn(policy);
        when(policy.find(1)).thenThrow(new CacheAccessException("get", new RuntimeException("down")));
        when(repository.findById(1)).thenReturn(Optional.of(schedule));

        ScheduleQueryService service = new ScheduleQueryService(registry, repository, metrics, properties());

        assertThat(service.find(1, CachePolicyType.CACHE_ASIDE)).isEqualTo(schedule);
    }

    private CacheProperties properties() {
        return new CacheProperties(
                "test:",
                Duration.ofMinutes(5),
                Duration.ZERO,
                Duration.ofSeconds(30),
                Duration.ofSeconds(30),
                Duration.ofMinutes(2),
                Duration.ofMillis(100),
                Duration.ofSeconds(1),
                1
        );
    }

    private StaffSchedule schedule() {
        return new StaffSchedule(
                1,
                "EMP-1001",
                "Emergency",
                LocalDate.of(2026, 6, 15),
                ShiftType.DAY,
                0,
                Instant.parse("2026-06-11T00:00:00Z")
        );
    }
}
