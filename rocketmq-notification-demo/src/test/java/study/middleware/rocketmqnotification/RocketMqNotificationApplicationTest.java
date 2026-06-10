package study.middleware.rocketmqnotification;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

@SpringBootTest(classes = {
        AppointmentController.class,
        AppointmentService.class,
        AppointmentRepository.class,
        NotificationRecordStore.class,
        SentinelGuard.class,
        RocketMqNotificationApplicationTest.TestPublisherConfiguration.class
})
class RocketMqNotificationApplicationTest {

    @Test
    void contextLoads() {
    }

    @TestConfiguration
    static class TestPublisherConfiguration {

        @Bean
        AppointmentEventPublisher appointmentEventPublisher() {
            return event -> {
            };
        }

        @Bean
        BusinessMetrics businessMetrics() {
            return new BusinessMetrics(new SimpleMeterRegistry());
        }

        @Bean
        JdbcTemplate jdbcTemplate() {
            DataSource dataSource = dataSource();
            new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
            return new JdbcTemplate(dataSource);
        }

        private DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    "jdbc:h2:mem:context-load-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                    "sa",
                    ""
            );
            dataSource.setDriverClassName("org.h2.Driver");
            return dataSource;
        }
    }
}
