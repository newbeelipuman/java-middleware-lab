package study.middleware.mqreliability.outbox;

import java.time.Instant;

public record OutboxEvent(
        long id,
        String eventId,
        String payloadJson,
        OutboxStatus status,
        String workerId,
        long fencingToken,
        int retryCount,
        Instant nextRetryAt
) {
}
