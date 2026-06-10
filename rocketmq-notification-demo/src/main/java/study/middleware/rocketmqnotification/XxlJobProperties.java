package study.middleware.rocketmqnotification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.xxl-job")
public record XxlJobProperties(
        boolean enabled,
        String adminAddresses,
        String appName,
        String ip,
        int port,
        String accessToken,
        String logPath,
        int logRetentionDays
) {
    public XxlJobProperties {
        if (adminAddresses == null || adminAddresses.isBlank()) {
            adminAddresses = "http://127.0.0.1:8080/xxl-job-admin";
        }
        if (appName == null || appName.isBlank()) {
            appName = "appointment-notification-executor";
        }
        if (ip == null) {
            ip = "";
        }
        if (port <= 0) {
            port = 9999;
        }
        if (accessToken == null) {
            accessToken = "";
        }
        if (logPath == null || logPath.isBlank()) {
            logPath = "logs/xxl-job";
        }
        if (logRetentionDays <= 0) {
            logRetentionDays = 7;
        }
    }
}
