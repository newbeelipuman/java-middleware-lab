package study.middleware.mqreliability.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import study.middleware.mqreliability.config.ReliabilityProperties;
import study.middleware.mqreliability.metrics.ReliabilityMetrics;

@Component
@ConditionalOnProperty(name = "app.publish-mode", havingValue = "outbox", matchIfMissing = true)
public class OutboxPublisher {
    private final OutboxRepository repository;
    private final RocketMQTemplate rocketMQTemplate;
    private final ReliabilityProperties properties;
    private final ReliabilityMetrics metrics;

    public OutboxPublisher(
            OutboxRepository repository,
            RocketMQTemplate rocketMQTemplate,
            ReliabilityProperties properties,
            ReliabilityMetrics metrics
    ) {
        this.repository = repository;
        this.rocketMQTemplate = rocketMQTemplate;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${app.outbox-delay:500}")
    public void publishBatch() {
        if (!properties.messagingEnabled()) return;
        List<OutboxEvent> claimed = repository.claim(properties.workerId(),
                properties.claimBatchSize(), properties.processingTimeout(), Instant.now());
        for (OutboxEvent event : claimed) {
            Instant started = Instant.now();
            try {
                SendResult result = rocketMQTemplate.syncSend(properties.destination(),
                        MessageBuilder.withPayload(event.payloadJson())
                                .setHeader("KEYS", event.eventId()).build());
                if (result == null || result.getSendStatus() == null) {
                    throw new IllegalStateException("RocketMQ returned no send status");
                }
                repository.markSent(event, Instant.now());
                metrics.count("mq_outbox_publish_total", "sent");
                metrics.record("mq_outbox_publish_duration", Duration.between(started, Instant.now()), "sent");
            } catch (RuntimeException exception) {
                repository.markFailed(event, exception, properties.maxRetries(), Instant.now());
                metrics.count("mq_outbox_publish_total", "failed");
            }
        }
    }
}
