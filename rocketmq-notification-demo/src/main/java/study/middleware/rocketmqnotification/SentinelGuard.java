package study.middleware.rocketmqnotification;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class SentinelGuard {

    private final BusinessMetrics metrics;

    @Autowired
    public SentinelGuard(BusinessMetrics metrics) {
        this.metrics = metrics;
    }

    SentinelGuard() {
        this(BusinessMetrics.noop());
    }

    public <T> T protect(String resource, Supplier<T> action) {
        Entry entry = null;
        try {
            entry = SphU.entry(resource);
            return action.get();
        } catch (BlockException ex) {
            metrics.recordSentinelBlock(resource);
            throw new SentinelRateLimitException(resource, ex.getRuleLimitApp(), ex);
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }
}
