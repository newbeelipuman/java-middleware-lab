package study.middleware.rocketmqnotification;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppointmentServiceTest {

    @Test
    void createsAppointmentAndPublishesEvent() {
        AppointmentRepository repository = mock(AppointmentRepository.class);
        AtomicReference<AppointmentCreatedEvent> published = new AtomicReference<>();
        Clock clock = Clock.fixed(Instant.parse("2026-06-03T00:00:00Z"), ZoneOffset.UTC);
        AppointmentService service = new AppointmentService(repository, published::set, clock);

        CreateAppointmentRequest request = new CreateAppointmentRequest(
                10L,
                20L,
                Instant.parse("2026-06-04T09:00:00Z")
        );
        Appointment stored = new Appointment(1001L, 10L, 20L, request.appointmentTime(), AppointmentStatus.CREATED);
        when(repository.saveNew(request)).thenReturn(stored);

        Appointment appointment = service.create(request);

        assertThat(appointment).isEqualTo(stored);
        assertThat(published.get().appointmentId()).isEqualTo(appointment.id());
        assertThat(published.get().patientId()).isEqualTo(10L);
        assertThat(published.get().occurredAt()).isEqualTo(Instant.parse("2026-06-03T00:00:00Z"));
        assertThat(published.get().eventId()).isNotBlank();
    }
}
