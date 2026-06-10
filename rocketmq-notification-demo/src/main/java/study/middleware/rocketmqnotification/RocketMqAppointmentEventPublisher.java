package study.middleware.rocketmqnotification;

import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class RocketMqAppointmentEventPublisher implements AppointmentEventPublisher {

    private final RocketMQTemplate rocketMQTemplate;
    private final String destination;

    public RocketMqAppointmentEventPublisher(
            RocketMQTemplate rocketMQTemplate,
            @Value("${app.rocketmq.appointment-topic}") String topic,
            @Value("${app.rocketmq.appointment-tag}") String tag
    ) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.destination = topic + ":" + tag;
    }

    @Override
    public void publish(AppointmentCreatedEvent event) {
        SendResult result = rocketMQTemplate.syncSend(
                destination,
                MessageBuilder.withPayload(event)
                        .setHeader("KEYS", event.eventId())
                        .build()
        );
        if (result == null || result.getSendStatus() == null) {
            throw new IllegalStateException("RocketMQ send did not return a valid result");
        }
    }
}
