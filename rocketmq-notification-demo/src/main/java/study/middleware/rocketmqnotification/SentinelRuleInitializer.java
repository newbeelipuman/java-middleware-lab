package study.middleware.rocketmqnotification;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SentinelRuleInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SentinelRuleInitializer.class);

    private final SentinelProperties properties;

    public SentinelRuleInitializer(SentinelProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            FlowRuleManager.loadRules(List.of());
            log.info("Sentinel protection disabled");
            return;
        }

        FlowRuleManager.loadRules(List.of(
                qpsRule(SentinelResources.APPOINTMENT_CREATE, properties.appointmentCreateQps()),
                qpsRule(SentinelResources.DOCTOR_SEARCH, properties.doctorSearchQps())
        ));
        log.info(
                "Sentinel rules loaded appointmentCreateQps={} doctorSearchQps={}",
                properties.appointmentCreateQps(),
                properties.doctorSearchQps()
        );
    }

    private FlowRule qpsRule(String resource, double count) {
        FlowRule rule = new FlowRule(resource);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(count);
        return rule;
    }
}
