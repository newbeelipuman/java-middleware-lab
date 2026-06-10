package study.middleware.rocketmqnotification;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XxlJobConfiguration {

    @Bean(destroyMethod = "destroy")
    @ConditionalOnProperty(prefix = "app.xxl-job", name = "enabled", havingValue = "true")
    public XxlJobSpringExecutor xxlJobExecutor(XxlJobProperties properties) {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(properties.adminAddresses());
        executor.setAppname(properties.appName());
        executor.setAddress("");
        executor.setIp(properties.ip());
        executor.setPort(properties.port());
        executor.setAccessToken(properties.accessToken());
        executor.setLogPath(properties.logPath());
        executor.setLogRetentionDays(properties.logRetentionDays());
        return executor;
    }
}
