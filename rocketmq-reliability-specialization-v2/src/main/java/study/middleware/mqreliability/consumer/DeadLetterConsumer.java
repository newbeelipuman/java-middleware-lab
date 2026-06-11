package study.middleware.mqreliability.consumer;

import java.nio.charset.StandardCharsets;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import study.middleware.mqreliability.quarantine.QuarantineRepository;
import study.middleware.mqreliability.metrics.ReliabilityMetrics;

@Component
@ConditionalOnProperty(name = "app.messaging-enabled", havingValue = "true", matchIfMissing = true)
@RocketMQMessageListener(
        topic = "%DLQ%${app.consumer-group}",
        consumerGroup = "${app.consumer-group}-dlq-inspector"
)
public class DeadLetterConsumer implements RocketMQListener<MessageExt> {
    private final QuarantineRepository quarantine;
    private final ReliabilityMetrics metrics;

    public DeadLetterConsumer(QuarantineRepository quarantine, ReliabilityMetrics metrics) {
        this.quarantine = quarantine;
        this.metrics = metrics;
    }

    @Override
    public void onMessage(MessageExt message) {
        quarantine.save(message.getMsgId(), message.getKeys(), "ROCKETMQ_DLQ",
                new String(message.getBody(), StandardCharsets.UTF_8));
        metrics.count("mq_dead_letter_total", "quarantined");
    }
}
