package study.middleware.rocketmqnotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRecordStore recordStore;
    private final BusinessMetrics metrics;
    private final Set<String> transientFailures = ConcurrentHashMap.newKeySet();
    private final Clock clock;

    @Autowired
    public NotificationService(NotificationRecordStore recordStore, BusinessMetrics metrics) {
        this(recordStore, metrics, Clock.systemUTC());
    }

    NotificationService(NotificationRecordStore recordStore, Clock clock) {
        this(recordStore, BusinessMetrics.noop(), clock);
    }

    NotificationService(NotificationRecordStore recordStore, BusinessMetrics metrics, Clock clock) {
        this.recordStore = recordStore;
        this.metrics = metrics;
        this.clock = clock;
    }

    public void handle(AppointmentCreatedEvent event) {
        if (recordStore.findByEventId(event.eventId()).isPresent()) {
            metrics.recordNotificationConsumption("duplicate");
            log.info("Skip duplicate appointment event eventId={} appointmentId={}", event.eventId(), event.appointmentId());
            return;
        }

        if (event.patientId() == 500L && transientFailures.add(event.eventId())) {
            metrics.recordNotificationConsumption("failed");
            log.warn("Simulating transient notification failure eventId={} appointmentId={}", event.eventId(), event.appointmentId());
            throw new NotificationDeliveryException("Simulated transient notification failure for retry drill");
        }

        Instant now = Instant.now(clock);
        NotificationRecord record = new NotificationRecord(
                event.eventId(),
                event.appointmentId(),
                event.patientId(),
                NotificationStatus.SENT,
                now
        );
        recordStore.saveIfAbsent(record);
        metrics.recordNotificationConsumption("sent");
        log.info("Notification recorded eventId={} appointmentId={} status={}", event.eventId(), event.appointmentId(), record.status());
    }
}
