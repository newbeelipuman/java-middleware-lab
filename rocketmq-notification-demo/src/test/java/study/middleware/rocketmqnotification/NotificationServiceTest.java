package study.middleware.rocketmqnotification;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-03T00:10:00Z"), ZoneOffset.UTC);
    private final NotificationRecordStore recordStore = new NotificationRecordStore(jdbcTemplate());
    private final NotificationService service = new NotificationService(recordStore, clock);

    @Test
    void recordsNotificationOnlyOnceForDuplicateEvent() {
        AppointmentCreatedEvent event = event("evt-1", 1001L, 10L);

        service.handle(event);
        service.handle(event);

        assertThat(recordStore.findByAppointmentId(1001L)).hasSize(1);
        assertThat(recordStore.findByEventId("evt-1"))
                .get()
                .extracting(NotificationRecord::status)
                .isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void transientFailureIsRetriedAndThenRecorded() {
        AppointmentCreatedEvent event = event("evt-retry", 1002L, 500L);

        assertThatThrownBy(() -> service.handle(event))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasMessageContaining("retry drill");
        assertThat(recordStore.findByEventId("evt-retry")).isEmpty();

        service.handle(event);

        assertThat(recordStore.findByEventId("evt-retry")).isPresent();
    }

    private AppointmentCreatedEvent event(String eventId, long appointmentId, long patientId) {
        return new AppointmentCreatedEvent(
                eventId,
                appointmentId,
                patientId,
                20L,
                Instant.parse("2026-06-04T09:00:00Z"),
                Instant.parse("2026-06-03T00:00:00Z")
        );
    }

    private static JdbcTemplate jdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:notification-service-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        dataSource.setDriverClassName("org.h2.Driver");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        return new JdbcTemplate(dataSource);
    }
}
