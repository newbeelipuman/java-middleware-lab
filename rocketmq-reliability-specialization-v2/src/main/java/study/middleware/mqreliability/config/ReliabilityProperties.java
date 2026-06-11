package study.middleware.mqreliability.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record ReliabilityProperties(
        String publishMode,
        boolean messagingEnabled,
        String topic,
        String tag,
        String consumerGroup,
        String workerId,
        int claimBatchSize,
        Duration processingTimeout,
        int maxRetries
) {
    public String destination() {
        return topic + ":" + tag;
    }
}
