package study.middleware.mqreliability.consumer;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import study.middleware.mqreliability.event.DomainEventEnvelope;
import study.middleware.mqreliability.event.ShiftChangePayload;
import study.middleware.mqreliability.notification.NotificationTaskService;
import study.middleware.mqreliability.quarantine.QuarantineRepository;
import study.middleware.mqreliability.shift.ShiftChangeApplicationService;

@Service
public class EventRouter {
    private final ObjectMapper objectMapper;
    private final NotificationTaskService notifications;
    private final QuarantineRepository quarantine;

    public EventRouter(
            ObjectMapper objectMapper,
            NotificationTaskService notifications,
            QuarantineRepository quarantine
    ) {
        this.objectMapper = objectMapper;
        this.notifications = notifications;
        this.quarantine = quarantine;
    }

    public void route(String sourceMessageId, String json, int redeliveryCount) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String eventId = text(root, "eventId");
            String eventType = text(root, "eventType");
            int version = root.path("schemaVersion").asInt(-1);
            if (!ShiftChangeApplicationService.EVENT_TYPE.equals(eventType) || version != 1) {
                quarantine.save(sourceMessageId, eventId, "UNSUPPORTED_EVENT_VERSION", json);
                return;
            }
            JavaType type = objectMapper.getTypeFactory()
                    .constructParametricType(DomainEventEnvelope.class, ShiftChangePayload.class);
            DomainEventEnvelope<ShiftChangePayload> event = objectMapper.readValue(json, type);
            notifications.handle(event, redeliveryCount);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            quarantine.save(sourceMessageId, null, "INVALID_JSON", json);
        }
    }

    private String text(JsonNode root, String name) {
        JsonNode node = root.get(name);
        return node == null || node.isNull() ? null : node.asText();
    }
}
