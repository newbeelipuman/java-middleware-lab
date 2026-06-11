package study.middleware.mqreliability.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.middleware.mqreliability.shift.ShiftChange;
import study.middleware.mqreliability.shift.ShiftChangeRepository;

@Component
@RocketMQTransactionListener
@ConditionalOnProperty(name = "app.publish-mode", havingValue = "transaction-message")
public class ShiftTransactionListener implements RocketMQLocalTransactionListener {
    private final ShiftChangeRepository shifts;
    private final TransactionFactRepository facts;
    private final ObjectMapper objectMapper;

    public ShiftTransactionListener(
            ShiftChangeRepository shifts,
            TransactionFactRepository facts,
            ObjectMapper objectMapper
    ) {
        this.shifts = shifts;
        this.facts = facts;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public RocketMQLocalTransactionState executeLocalTransaction(Message message, Object argument) {
        if (!(argument instanceof TransactionContext context)) {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
        ShiftChange shift = shifts.insert(context.aggregateId(), context.request(), java.time.Instant.now());
        facts.committed(context.eventId(), shift.id());
        context.result(shift);
        return RocketMQLocalTransactionState.COMMIT;
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message message) {
        try {
            return facts.state(eventId(message))
                    .map(state -> "COMMITTED".equals(state)
                            ? RocketMQLocalTransactionState.COMMIT
                            : RocketMQLocalTransactionState.ROLLBACK)
                    .orElse(RocketMQLocalTransactionState.ROLLBACK);
        } catch (DataAccessException exception) {
            return RocketMQLocalTransactionState.UNKNOWN;
        }
    }

    private String eventId(Message message) {
        try {
            Object payload = message.getPayload();
            String json = payload instanceof byte[] bytes
                    ? new String(bytes, StandardCharsets.UTF_8)
                    : String.valueOf(payload);
            return objectMapper.readTree(json).path("eventId").asText();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Transaction message has no readable eventId", exception);
        }
    }
}
