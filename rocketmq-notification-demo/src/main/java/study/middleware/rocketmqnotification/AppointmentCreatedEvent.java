package study.middleware.rocketmqnotification;

import java.time.Instant;

public record AppointmentCreatedEvent(
        String eventId,
        long appointmentId,
        long patientId,
        long doctorId,
        Instant appointmentTime,
        Instant occurredAt
) {
}
