package study.middleware.rediscache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RedisProductCache implements ProductCache {

    private static final String KEY_PREFIX = "demo:product:";
    private static final String MISSING_VALUE = "__MISSING__";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final Duration missingTtl;
    private final Duration ttlJitter;

    public RedisProductCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${demo.cache.product-ttl}") Duration ttl,
            @Value("${demo.cache.product-missing-ttl}") Duration missingTtl,
            @Value("${demo.cache.product-ttl-jitter}") Duration ttlJitter) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
        this.missingTtl = missingTtl;
        this.ttlJitter = ttlJitter;
    }

    @Override
    public Optional<Product> find(long id) {
        String json = redisTemplate.opsForValue().get(key(id));
        if (json == null || MISSING_VALUE.equals(json)) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json, Product.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to read cached product " + id, exception);
        }
    }

    @Override
    public boolean isKnownMissing(long id) {
        return MISSING_VALUE.equals(redisTemplate.opsForValue().get(key(id)));
    }

    @Override
    public void put(Product product) {
        try {
            redisTemplate.opsForValue().set(
                    key(product.id()),
                    objectMapper.writeValueAsString(product),
                    ttlWithJitter());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to cache product " + product.id(), exception);
        }
    }

    @Override
    public void putMissing(long id) {
        redisTemplate.opsForValue().set(key(id), MISSING_VALUE, missingTtl);
    }

    @Override
    public void evict(long id) {
        redisTemplate.delete(key(id));
    }

    Duration ttlWithJitter() {
        long jitterMillis = ttlJitter.toMillis();
        if (jitterMillis <= 0) {
            return ttl;
        }
        return ttl.plusMillis(ThreadLocalRandom.current().nextLong(jitterMillis + 1));
    }

    private String key(long id) {
        return KEY_PREFIX + id;
    }
}
