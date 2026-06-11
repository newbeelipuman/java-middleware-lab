package study.middleware.mqreliability.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class ReliabilityMetrics {
    private final MeterRegistry registry;

    public ReliabilityMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void count(String metric, String result) {
        registry.counter(metric, "result", result).increment();
    }

    public void record(String metric, Duration duration, String result) {
        Timer.builder(metric).tag("result", result).register(registry).record(duration);
    }
}
