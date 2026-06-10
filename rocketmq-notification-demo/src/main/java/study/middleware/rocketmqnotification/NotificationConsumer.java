package study.middleware.rocketmqnotification;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
        topic = "${app.rocketmq.appointment-topic}",
        selectorExpression = "${app.rocketmq.appointment-tag}",
        consumerGroup = "appointment-notification-consumer"
)
public class NotificationConsumer implements RocketMQListener<AppointmentCreatedEvent> {

    private final NotificationService notificationService;

    public NotificationConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onMessage(AppointmentCreatedEvent event) {
        notificationService.handle(event);
    }
}
