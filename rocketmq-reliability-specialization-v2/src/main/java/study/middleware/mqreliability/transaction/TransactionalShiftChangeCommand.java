package study.middleware.mqreliability.transaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import study.middleware.mqreliability.config.ReliabilityProperties;
import study.middleware.mqreliability.event.DomainEventEnvelope;
import study.middleware.mqreliability.event.ShiftChangePayload;
import study.middleware.mqreliability.shift.CreateShiftChangeRequest;
import study.middleware.mqreliability.shift.ShiftChange;
import study.middleware.mqreliability.shift.ShiftChangeApplicationService;
import study.middleware.mqreliability.shift.ShiftChangeCommand;

@Service
@ConditionalOnProperty(name = "app.publish-mode", havingValue = "transaction-message")
public class TransactionalShiftChangeCommand implements ShiftChangeCommand {
    private final RocketMQTemplate template;
    private final ReliabilityProperties properties;
    private final ObjectMapper objectMapper;

    public TransactionalShiftChangeCommand(
            RocketMQTemplate template,
            ReliabilityProperties properties,
            ObjectMapper objectMapper
    ) {
        this.template = template;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public ShiftChange create(CreateShiftChangeRequest request) {
        Instant now = Instant.now();
        String eventId = UUID.randomUUID().toString();
        String aggregateId = UUID.randomUUID().toString();
        DomainEventEnvelope<ShiftChangePayload> provisional = new DomainEventEnvelope<>(
                eventId, ShiftChangeApplicationService.EVENT_TYPE, "ShiftChange", aggregateId,
                now, 1, null, new ShiftChangePayload(aggregateId, request.staffCode(),
                request.fromShift(), request.toShift()));
        TransactionContext context = new TransactionContext(eventId, aggregateId, request);
        try {
            TransactionSendResult result = template.sendMessageInTransaction(
                    properties.destination(),
                    MessageBuilder.withPayload(objectMapper.writeValueAsString(provisional))
                            .setHeader("KEYS", eventId).build(),
                    context);
            if (result == null || context.result() == null) {
                throw new IllegalStateException("Local transaction did not commit");
            }
            return context.result();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize transaction message", exception);
        }
    }
}
