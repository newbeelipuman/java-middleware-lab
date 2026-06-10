package study.middleware.rocketmqnotification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryJdbcTest {

    private AppointmentRepository appointmentRepository;
    private NotificationRecordStore notificationRecordStore;
    private DoctorRepository doctorRepository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:repository-jdbc-test-" + System.nanoTime()
                        + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        dataSource.setDriverClassName("org.h2.Driver");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        appointmentRepository = new AppointmentRepository(jdbcTemplate);
        notificationRecordStore = new NotificationRecordStore(jdbcTemplate);
        doctorRepository = new DoctorRepository(jdbcTemplate);
    }

    @Test
    void savesAndFindsAppointment() {
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                10L,
                20L,
                Instant.parse("2026-06-04T09:00:00Z")
        );

        Appointment appointment = appointmentRepository.saveNew(request);

        assertThat(appointment.id()).isPositive();
        assertThat(appointmentRepository.find(appointment.id())).contains(appointment);
    }

    @Test
    void eventIdUniqueIndexPreventsDuplicateNotificationRecord() {
        NotificationRecord record = new NotificationRecord(
                "evt-duplicate",
                1001L,
                10L,
                NotificationStatus.SENT,
                Instant.parse("2026-06-03T00:00:00Z")
        );

        assertThat(notificationRecordStore.saveIfAbsent(record)).isTrue();
        assertThat(notificationRecordStore.saveIfAbsent(record)).isFalse();

        assertThat(notificationRecordStore.findByAppointmentId(1001L)).containsExactly(record);
        assertThat(notificationRecordStore.findByEventId("evt-duplicate")).contains(record);
    }

    @Test
    void savesAndListsDoctorsFromMysql() {
        Doctor doctor = new Doctor(
                null,
                "Li Ming",
                "Cardiology",
                "hypertension and coronary disease",
                true,
                Instant.parse("2026-06-03T00:00:00Z")
        );

        Doctor saved = doctorRepository.saveNew(doctor);

        assertThat(saved.id()).isPositive();
        assertThat(doctorRepository.count()).isEqualTo(1);
        assertThat(doctorRepository.findAll()).containsExactly(saved);
    }
}
