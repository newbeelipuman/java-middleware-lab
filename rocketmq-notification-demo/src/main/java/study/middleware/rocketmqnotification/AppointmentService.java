package study.middleware.rocketmqnotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository repository;
    private final AppointmentEventPublisher eventPublisher;
    private final BusinessMetrics metrics;
    private final Clock clock;

    @Autowired
    public AppointmentService(AppointmentRepository repository, AppointmentEventPublisher eventPublisher, BusinessMetrics metrics) {
        this(repository, eventPublisher, metrics, Clock.systemUTC());
    }

    AppointmentService(AppointmentRepository repository, AppointmentEventPublisher eventPublisher, Clock clock) {
        this(repository, eventPublisher, BusinessMetrics.noop(), clock);
    }

    AppointmentService(
            AppointmentRepository repository,
            AppointmentEventPublisher eventPublisher,
            BusinessMetrics metrics,
            Clock clock
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
        this.clock = clock;
    }

    public Appointment create(CreateAppointmentRequest request) {
        Appointment appointment = repository.saveNew(request);
        Instant now = Instant.now(clock);
        AppointmentCreatedEvent event = new AppointmentCreatedEvent(
                UUID.randomUUID().toString(),
                appointment.id(),
                appointment.patientId(),
                appointment.doctorId(),
                appointment.appointmentTime(),
                now
        );
        try {
            eventPublisher.publish(event);
            metrics.recordAppointmentEventPublished("success");
        } catch (RuntimeException ex) {
            metrics.recordAppointmentEventPublished("failed");
            throw ex;
        }
        log.info("Published appointment event eventId={} appointmentId={}", event.eventId(), appointment.id());
        return appointment;
    }
}
