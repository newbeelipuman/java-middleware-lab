package study.middleware.rocketmqnotification;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SentinelGuardTest {

    private final SentinelGuard guard = new SentinelGuard();

    @AfterEach
    void clearRules() {
        FlowRuleManager.loadRules(List.of());
    }

    @Test
    void allowsRequestWhenResourceIsUnderQpsLimit() {
        String result = guard.protect("test.sentinel.allow", () -> "ok");

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void throwsRateLimitExceptionWhenQpsLimitIsExceeded() {
        String resource = "test.sentinel.block";
        FlowRuleManager.loadRules(List.of(qpsRule(resource, 1)));
        AtomicInteger executed = new AtomicInteger();

        assertThat(guard.protect(resource, executed::incrementAndGet)).isEqualTo(1);

        assertThatThrownBy(() -> guard.protect(resource, executed::incrementAndGet))
                .isInstanceOf(SentinelRateLimitException.class)
                .hasMessageContaining(resource);
        assertThat(executed).hasValue(1);
    }

    private FlowRule qpsRule(String resource, double count) {
        FlowRule rule = new FlowRule(resource);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(count);
        return rule;
    }
}
