package study.middleware.redisspecialization.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import study.middleware.redisspecialization.config.CacheProperties;
import study.middleware.redisspecialization.schedule.StaffSchedule;

@Component
public class ScheduleCacheStore {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheProperties properties;

    public ScheduleCacheStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            CacheProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String key(long scheduleId) {
        return properties.keyPrefix() + scheduleId;
    }

    public Optional<CachedSchedule> get(long scheduleId) {
        try {
            String json = redisTemplate.opsForValue().get(key(scheduleId));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, CachedSchedule.class));
        } catch (RuntimeException | JsonProcessingException exception) {
            throw new CacheAccessException("get", exception);
        }
    }

    public void put(StaffSchedule schedule) {
        Instant now = Instant.now();
        write(
                schedule.id(),
                new CachedSchedule(schedule, false, now, null),
                jittered(properties.ttl())
        );
    }

    public void putMissing(long scheduleId) {
        Instant now = Instant.now();
        write(scheduleId, new CachedSchedule(null, true, now, null), properties.missingTtl());
    }

    public void putLogical(StaffSchedule schedule) {
        Instant now = Instant.now();
        Duration physicalTtl = properties.logicalTtl().plus(properties.logicalStaleLimit());
        write(
                schedule.id(),
                new CachedSchedule(schedule, false, now, now.plus(properties.logicalTtl())),
                physicalTtl
        );
    }

    public void delete(String cacheKey) {
        try {
            redisTemplate.delete(cacheKey);
        } catch (RuntimeException exception) {
            throw new CacheAccessException("delete", exception);
        }
    }

    private void write(long scheduleId, CachedSchedule value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key(scheduleId), objectMapper.writeValueAsString(value), ttl);
        } catch (RuntimeException | JsonProcessingException exception) {
            throw new CacheAccessException("put", exception);
        }
    }

    private Duration jittered(Duration base) {
        long maxJitterMillis = properties.ttlJitter().toMillis();
        if (maxJitterMillis <= 0) {
            return base;
        }
        return base.plusMillis(ThreadLocalRandom.current().nextLong(maxJitterMillis + 1));
    }
}
