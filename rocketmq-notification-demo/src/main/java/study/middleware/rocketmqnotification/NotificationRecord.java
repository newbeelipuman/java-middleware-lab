package study.middleware.rocketmqnotification;

import java.time.Instant;

public record NotificationRecord(
        String eventId,
        long appointmentId,
        long patientId,
        NotificationStatus status,
        Instant handledAt
) {
}
