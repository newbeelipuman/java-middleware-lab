package study.middleware.redisspecialization.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.redisson")
public record RedissonProperties(String address) {
}
