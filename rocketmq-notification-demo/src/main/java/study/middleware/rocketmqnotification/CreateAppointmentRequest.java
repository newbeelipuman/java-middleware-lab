package study.middleware.rocketmqnotification;

import java.time.Instant;

public record CreateAppointmentRequest(
        long patientId,
        long doctorId,
        Instant appointmentTime
) {
}
