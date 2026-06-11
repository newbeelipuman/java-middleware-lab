package study.middleware.mqreliability.shift;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import study.middleware.mqreliability.event.DomainEventEnvelope;
import study.middleware.mqreliability.event.ShiftChangePayload;
import study.middleware.mqreliability.outbox.OutboxRepository;

@Service
@ConditionalOnProperty(name = "app.publish-mode", havingValue = "outbox", matchIfMissing = true)
public class ShiftChangeApplicationService implements ShiftChangeCommand {
    public static final String EVENT_TYPE = "ShiftChangeRequested";
    private final ShiftChangeRepository shifts;
    private final OutboxRepository outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock = Clock.systemUTC();

    public ShiftChangeApplicationService(
            ShiftChangeRepository shifts,
            OutboxRepository outbox,
            ObjectMapper objectMapper
    ) {
        this.shifts = shifts;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ShiftChange create(CreateShiftChangeRequest request) {
        Instant now = Instant.now(clock);
        ShiftChange shift = shifts.insert(request, now);
        DomainEventEnvelope<ShiftChangePayload> event = envelope(shift, now);
        try {
            outbox.insert(event, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize outbox event", exception);
        }
        return shift;
    }

    public static DomainEventEnvelope<ShiftChangePayload> envelope(ShiftChange shift, Instant now) {
        String traceId = MDC.get("traceId");
        return new DomainEventEnvelope<>(
                UUID.randomUUID().toString(), EVENT_TYPE, "ShiftChange", shift.id(),
                now, 1, traceId, new ShiftChangePayload(
                shift.id(), shift.staffCode(), shift.fromShift(), shift.toShift()));
    }
}
