package study.middleware.rocketmqnotification;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {

    static final String APPOINTMENT_EVENTS = "appointment.events.published";
    static final String NOTIFICATION_CONSUMPTION = "notification.consumption";
    static final String COMPENSATION_RUNS = "compensation.runs";
    static final String COMPENSATION_DURATION = "compensation.duration";
    static final String SENTINEL_BLOCKS = "sentinel.blocks";

    private final MeterRegistry meterRegistry;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public static BusinessMetrics noop() {
        return new BusinessMetrics(new SimpleMeterRegistry());
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordAppointmentEventPublished(String result) {
        counter(APPOINTMENT_EVENTS, "result", result).increment();
    }

    public void recordNotificationConsumption(String result) {
        counter(NOTIFICATION_CONSUMPTION, "result", result).increment();
    }

    public void recordCompensationRun(String jobName, String result, Timer.Sample sample) {
        counter(COMPENSATION_RUNS, "job", jobName, "result", result).increment();
        sample.stop(Timer.builder(COMPENSATION_DURATION)
                .tag("job", jobName)
                .tag("result", result)
                .register(meterRegistry));
    }

    public void recordSentinelBlock(String resource) {
        counter(SENTINEL_BLOCKS, "resource", resource).increment();
    }

    private Counter counter(String name, String... tags) {
        return Counter.builder(name).tags(tags).register(meterRegistry);
    }
}
