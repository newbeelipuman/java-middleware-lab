package study.middleware.rocketmqnotification;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class NacosRegistrationRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(NacosRegistrationRunner.class);

    private final NacosProperties nacosProperties;
    private final NacosClientFactory nacosClientFactory;
    private final int serverPort;
    private final AtomicBoolean registered = new AtomicBoolean(false);

    public NacosRegistrationRunner(
            NacosProperties nacosProperties,
            NacosClientFactory nacosClientFactory,
            @Value("${server.port}") int serverPort
    ) {
        this.nacosProperties = nacosProperties;
        this.nacosClientFactory = nacosClientFactory;
        this.serverPort = serverPort;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!nacosProperties.enabled()) {
            return;
        }
        try {
            Instance instance = new Instance();
            instance.setIp(nacosProperties.instanceIp());
            instance.setPort(serverPort);
            instance.setEphemeral(true);
            instance.setHealthy(true);
            instance.setMetadata(Map.of(
                    "stage", "middleware-stage-3-nacos-services",
                    "boundaries", "appointment,notification,search"
            ));
            NamingService namingService = nacosClientFactory.namingService();
            namingService.registerInstance(nacosProperties.serviceName(), nacosProperties.group(), instance);
            registered.set(true);
            LOGGER.info("Registered service to Nacos serviceName={} group={} ip={} port={}",
                    nacosProperties.serviceName(), nacosProperties.group(), nacosProperties.instanceIp(), serverPort);
        } catch (NacosException e) {
            LOGGER.warn("Failed to register service to Nacos serverAddr={}", nacosProperties.serverAddr(), e);
        }
    }

    public boolean registered() {
        return registered.get();
    }
}
