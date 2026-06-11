package study.middleware.mqreliability.quarantine;

import java.time.Instant;

public record QuarantineRecord(
        long id,
        String sourceMessageId,
        String eventId,
        String reason,
        String payloadJson,
        int replayCount,
        Instant createdAt
) {
}
