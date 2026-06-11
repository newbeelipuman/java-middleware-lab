package study.middleware.redisspecialization.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.cache")
public record CacheProperties(
        String keyPrefix,
        Duration ttl,
        Duration ttlJitter,
        Duration missingTtl,
        Duration logicalTtl,
        Duration logicalStaleLimit,
        Duration lockWait,
        Duration lockLease,
        int databaseFallbackPermits
) {
}
