package study.middleware.redisspecialization.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
class RedissonConfiguration {

    @Bean(destroyMethod = "shutdown")
    @Lazy
    RedissonClient redissonClient(RedissonProperties properties) {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(properties.address())
                .setConnectTimeout(500)
                .setTimeout(500)
                .setRetryAttempts(1);
        return Redisson.create(config);
    }
}
