package study.middleware.redisspecialization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import study.middleware.redisspecialization.cache.CachePolicyType;
import study.middleware.redisspecialization.invalidation.InvalidationTaskProcessor;
import study.middleware.redisspecialization.schedule.ScheduleCommandService;
import study.middleware.redisspecialization.schedule.ScheduleQueryService;
import study.middleware.redisspecialization.schedule.ScheduleUpdateResult;
import study.middleware.redisspecialization.schedule.ShiftType;
import study.middleware.redisspecialization.schedule.StaffSchedule;
import study.middleware.redisspecialization.schedule.UpdateScheduleRequest;

@SpringBootTest(properties = "app.invalidation.scheduling-enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class RedisSpecializationIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("redis_cache_specialization_v2")
            .withUsername("redis_v2")
            .withPassword("redis_v2");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add(
                "app.redisson.address",
                () -> "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379)
        );
    }

    @Autowired
    ScheduleQueryService queryService;

    @Autowired
    ScheduleCommandService commandService;

    @Autowired
    InvalidationTaskProcessor invalidationProcessor;

    @Test
    void readsThroughCacheAndInvalidatesAfterTransactionalUpdate() {
        StaffSchedule initial = queryService.find(1, CachePolicyType.CACHE_ASIDE);
        assertThat(initial.shiftType()).isEqualTo(ShiftType.DAY);

        ScheduleUpdateResult updated = commandService.update(
                1,
                new UpdateScheduleRequest("Emergency", ShiftType.NIGHT)
        );
        invalidationProcessor.process(updated.invalidationTaskId());

        StaffSchedule reloaded = queryService.find(1, CachePolicyType.CACHE_ASIDE);
        assertThat(reloaded.shiftType()).isEqualTo(ShiftType.NIGHT);
        assertThat(reloaded.version()).isEqualTo(initial.version() + 1);
    }
}
