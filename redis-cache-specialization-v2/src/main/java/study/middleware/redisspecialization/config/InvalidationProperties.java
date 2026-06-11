package study.middleware.redisspecialization.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.invalidation")
public record InvalidationProperties(int batchSize, int maxRetries) {
}
