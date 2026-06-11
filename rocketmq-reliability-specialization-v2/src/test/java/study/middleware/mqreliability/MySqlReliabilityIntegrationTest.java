package study.middleware.mqreliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import study.middleware.mqreliability.event.DomainEventEnvelope;
import study.middleware.mqreliability.event.ShiftChangePayload;
import study.middleware.mqreliability.notification.DownstreamDeliveryService;
import study.middleware.mqreliability.notification.DownstreamResultUnknownException;
import study.middleware.mqreliability.notification.NotificationChannel;
import study.middleware.mqreliability.outbox.OutboxEvent;
import study.middleware.mqreliability.outbox.OutboxRepository;
import study.middleware.mqreliability.shift.CreateShiftChangeRequest;
import study.middleware.mqreliability.shift.ShiftChange;
import study.middleware.mqreliability.shift.ShiftChangeRepository;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MySqlReliabilityIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("mq_v2").withUsername("test").withPassword("test");

    private JdbcTemplate jdbc;
    private ShiftChangeRepository shifts;
    private OutboxRepository outbox;
    private TransactionTemplate transaction;

    @BeforeAll
    void setup() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        shifts = new ShiftChangeRepository(jdbc);
        outbox = new OutboxRepository(jdbc);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM downstream_delivery");
        jdbc.update("DELETE FROM notification_task");
        jdbc.update("DELETE FROM outbox_event");
        jdbc.update("DELETE FROM shift_change_request");
    }

    @Test
    void businessAndOutboxCommitTogether() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        OutboxRepository txOutbox = new OutboxRepository(new JdbcTemplate(dataSource));
        ShiftChangeRepository txShifts = new ShiftChangeRepository(new JdbcTemplate(dataSource));
        Instant now = Instant.now();

        transaction.executeWithoutResult(ignored -> {
            ShiftChange shift = txShifts.insert(new CreateShiftChangeRequest("S-100", "DAY", "NIGHT"), now);
            DomainEventEnvelope<ShiftChangePayload> event = new DomainEventEnvelope<>(
                    "evt-atomic", "ShiftChangeRequested", "ShiftChange", shift.id(),
                    now, 1, "trace", new ShiftChangePayload(shift.id(), "S-100", "DAY", "NIGHT"));
            try {
                txOutbox.insert(event, new ObjectMapper().registerModule(new JavaTimeModule())
                        .writeValueAsString(event));
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        });

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE event_id='evt-atomic'", Integer.class)).isOne();
    }

    @Test
    void timeoutTakeoverFencesOldWorker() {
        Instant now = Instant.now();
        insertOutbox("evt-fence", now);
        OutboxEvent first = outbox.claim("worker-a", 1, Duration.ofMillis(10), now).get(0);
        jdbc.update("UPDATE outbox_event SET processing_deadline=? WHERE id=?",
                java.sql.Timestamp.from(now.minusSeconds(1)), first.id());
        OutboxEvent second = outbox.claim("worker-b", 1, Duration.ofSeconds(10), now).get(0);

        assertThat(second.fencingToken()).isGreaterThan(first.fencingToken());
        assertThat(outbox.markSent(first, Instant.now())).isFalse();
        assertThat(outbox.markSent(second, Instant.now())).isTrue();
    }

    @Test
    void concurrentWorkersClaimDifferentRows() throws Exception {
        insertOutbox("evt-c1", Instant.now());
        insertOutbox("evt-c2", Instant.now());
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> {
                start.await();
                return transaction.execute(ignored ->
                        outbox.claim("worker-c1", 1, Duration.ofSeconds(10), Instant.now()));
            });
            var b = executor.submit(() -> {
                start.await();
                return transaction.execute(ignored ->
                        outbox.claim("worker-c2", 1, Duration.ofSeconds(10), Instant.now()));
            });
            start.countDown();
            assertThat(a.get().get(0).id()).isNotEqualTo(b.get().get(0).id());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void downstreamTimeoutRetryUsesPersistentIdempotencyRecord() {
        DownstreamDeliveryService downstream = new DownstreamDeliveryService(jdbc);
        ShiftChangePayload payload = new ShiftChangePayload("1", "TIMEOUT_ONCE", "DAY", "NIGHT");

        assertThatThrownBy(() -> downstream.deliver("evt-timeout", NotificationChannel.EMAIL, payload))
                .isInstanceOf(DownstreamResultUnknownException.class);
        downstream.deliver("evt-timeout", NotificationChannel.EMAIL, payload);

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM downstream_delivery
                WHERE idempotency_key='evt-timeout:EMAIL'
                """, Integer.class)).isOne();
    }

    private void insertOutbox(String eventId, Instant now) {
        DomainEventEnvelope<ShiftChangePayload> event = new DomainEventEnvelope<>(
                eventId, "ShiftChangeRequested", "ShiftChange", "1", now, 1, null,
                new ShiftChangePayload("1", "S-1", "DAY", "NIGHT"));
        outbox.insert(event, "{}");
    }
}
