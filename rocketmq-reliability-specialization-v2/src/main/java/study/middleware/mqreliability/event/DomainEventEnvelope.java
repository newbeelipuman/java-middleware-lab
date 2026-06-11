package study.middleware.mqreliability.event;

import java.time.Instant;

public record DomainEventEnvelope<T>(
        String eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        int schemaVersion,
        String traceId,
        T payload
) {
}
