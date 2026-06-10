package study.middleware.rocketmqnotification;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/nacos")
public class NacosStatusController {

    private final NacosProperties nacosProperties;
    private final NacosRegistrationRunner nacosRegistrationRunner;
    private final SearchSettings searchSettings;

    public NacosStatusController(
            NacosProperties nacosProperties,
            NacosRegistrationRunner nacosRegistrationRunner,
            SearchSettings searchSettings
    ) {
        this.nacosProperties = nacosProperties;
        this.nacosRegistrationRunner = nacosRegistrationRunner;
        this.searchSettings = searchSettings;
    }

    @GetMapping("/status")
    public NacosStatus status() {
        return new NacosStatus(
                nacosProperties.enabled(),
                nacosRegistrationRunner.registered(),
                nacosProperties.serverAddr(),
                nacosProperties.serviceName(),
                nacosProperties.configDataId(),
                searchSettings.defaultPageSize(),
                List.of("appointment", "notification", "search")
        );
    }
}
