package study.middleware.rocketmqnotification;

import java.time.Instant;

public record Appointment(
        long id,
        long patientId,
        long doctorId,
        Instant appointmentTime,
        AppointmentStatus status
) {
}
