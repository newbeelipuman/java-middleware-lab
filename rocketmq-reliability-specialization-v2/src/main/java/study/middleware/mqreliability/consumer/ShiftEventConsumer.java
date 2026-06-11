package study.middleware.mqreliability.consumer;

import java.nio.charset.StandardCharsets;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging-enabled", havingValue = "true", matchIfMissing = true)
@RocketMQMessageListener(
        topic = "${app.topic}",
        selectorExpression = "${app.tag}",
        consumerGroup = "${app.consumer-group}",
        maxReconsumeTimes = 2,
        delayLevelWhenNextConsume = 1
)
public class ShiftEventConsumer implements RocketMQListener<MessageExt> {
    private final EventRouter router;

    public ShiftEventConsumer(EventRouter router) {
        this.router = router;
    }

    @Override
    public void onMessage(MessageExt message) {
        router.route(message.getMsgId(), new String(message.getBody(), StandardCharsets.UTF_8),
                message.getReconsumeTimes());
    }
}
