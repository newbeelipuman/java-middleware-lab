package study.middleware.rocketmqnotification;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessMetricsTest {

    @Test
    void recordsBusinessCountersAndCompensationTimer() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        BusinessMetrics metrics = new BusinessMetrics(meterRegistry);

        metrics.recordAppointmentEventPublished("success");
        metrics.recordNotificationConsumption("sent");
        metrics.recordNotificationConsumption("failed");
        metrics.recordSentinelBlock("doctor.search");
        Timer.Sample sample = metrics.startTimer();
        metrics.recordCompensationRun("doctorIndexCompensationJob", "completed", sample);

        assertThat(counter(meterRegistry, BusinessMetrics.APPOINTMENT_EVENTS, "result", "success")).isEqualTo(1.0);
        assertThat(counter(meterRegistry, BusinessMetrics.NOTIFICATION_CONSUMPTION, "result", "sent")).isEqualTo(1.0);
        assertThat(counter(meterRegistry, BusinessMetrics.NOTIFICATION_CONSUMPTION, "result", "failed")).isEqualTo(1.0);
        assertThat(counter(meterRegistry, BusinessMetrics.SENTINEL_BLOCKS, "resource", "doctor.search")).isEqualTo(1.0);
        assertThat(counter(
                meterRegistry,
                BusinessMetrics.COMPENSATION_RUNS,
                "job",
                "doctorIndexCompensationJob",
                "result",
                "completed"
        )).isEqualTo(1.0);
        assertThat(meterRegistry.find(BusinessMetrics.COMPENSATION_DURATION)
                .tag("job", "doctorIndexCompensationJob")
                .tag("result", "completed")
                .timer()
                .count()).isEqualTo(1L);
    }

    private double counter(SimpleMeterRegistry meterRegistry, String name, String... tags) {
        return meterRegistry.find(name).tags(tags).counter().count();
    }
}
