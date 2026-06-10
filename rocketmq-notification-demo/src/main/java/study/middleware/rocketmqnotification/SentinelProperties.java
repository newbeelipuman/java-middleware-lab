package study.middleware.rocketmqnotification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sentinel")
public record SentinelProperties(
        boolean enabled,
        double appointmentCreateQps,
        double doctorSearchQps
) {
    public SentinelProperties {
        if (appointmentCreateQps <= 0) {
            appointmentCreateQps = 2;
        }
        if (doctorSearchQps <= 0) {
            doctorSearchQps = 3;
        }
    }
}
