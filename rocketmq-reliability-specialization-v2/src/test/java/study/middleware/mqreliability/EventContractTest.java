package study.middleware.mqreliability;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import study.middleware.mqreliability.event.DomainEventEnvelope;
import study.middleware.mqreliability.event.ShiftChangePayload;

class EventContractTest {
    @Test
    void envelopeContainsStableContractFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        DomainEventEnvelope<ShiftChangePayload> event = new DomainEventEnvelope<>(
                "evt-1", "ShiftChangeRequested", "ShiftChange", "10",
                Instant.parse("2026-06-11T10:00:00Z"), 1, "trace-1",
                new ShiftChangePayload("10", "S-001", "DAY", "NIGHT"));

        String json = mapper.writeValueAsString(event);

        assertThat(json).contains("\"eventId\":\"evt-1\"", "\"schemaVersion\":1",
                "\"traceId\":\"trace-1\"", "\"staffCode\":\"S-001\"");
    }
}
