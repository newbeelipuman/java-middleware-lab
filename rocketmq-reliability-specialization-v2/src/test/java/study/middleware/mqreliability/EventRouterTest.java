package study.middleware.mqreliability;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import study.middleware.mqreliability.consumer.EventRouter;
import study.middleware.mqreliability.notification.NotificationTaskService;
import study.middleware.mqreliability.quarantine.QuarantineRepository;

class EventRouterTest {
    private final NotificationTaskService notifications = Mockito.mock(NotificationTaskService.class);
    private final QuarantineRepository quarantine = Mockito.mock(QuarantineRepository.class);
    private final EventRouter router = new EventRouter(
            new ObjectMapper().registerModule(new JavaTimeModule()), notifications, quarantine);

    @Test
    void unsupportedVersionIsQuarantinedAndAcknowledged() {
        String json = """
                {"eventId":"evt-2","eventType":"ShiftChangeRequested","schemaVersion":99,"payload":{}}
                """;

        router.route("msg-2", json, 0);

        verify(quarantine).save("msg-2", "evt-2", "UNSUPPORTED_EVENT_VERSION", json);
        verifyNoInteractions(notifications);
    }

    @Test
    void invalidJsonIsQuarantined() {
        router.route("msg-bad", "{", 0);
        verify(quarantine).save("msg-bad", null, "INVALID_JSON", "{");
    }
}
