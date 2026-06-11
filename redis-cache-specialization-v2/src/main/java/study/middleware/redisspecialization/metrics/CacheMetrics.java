package study.middleware.redisspecialization.metrics;

import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CacheMetrics {

    private final MeterRegistry registry;

    public CacheMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void record(String result, String policy) {
        registry.counter("cache.requests", "result", result, "policy", policy).increment();
    }

    public void recordDatabaseLoad() {
        registry.counter("cache.database.loads").increment();
    }

    public void recordLockWait(String policy, long nanos) {
        registry.timer("cache.lock.wait", "policy", policy).record(nanos, TimeUnit.NANOSECONDS);
    }

    public void recordInvalidation(String result) {
        registry.counter("cache.invalidation", "result", result).increment();
    }

    public void recordDegraded(String result) {
        registry.counter("cache.degraded", "result", result).increment();
    }
}
