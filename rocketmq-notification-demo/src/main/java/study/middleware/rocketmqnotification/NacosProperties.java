package study.middleware.rocketmqnotification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.nacos")
public record NacosProperties(
        boolean enabled,
        String serverAddr,
        String group,
        String serviceName,
        String configDataId,
        String instanceIp
) {
}
