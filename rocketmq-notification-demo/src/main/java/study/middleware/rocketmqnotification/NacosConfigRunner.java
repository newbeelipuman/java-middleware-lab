package study.middleware.rocketmqnotification;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.Properties;
import java.util.concurrent.Executor;

@Component
public class NacosConfigRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(NacosConfigRunner.class);
    private static final String DEFAULT_PAGE_SIZE_KEY = "app.search.default-page-size";

    private final NacosProperties nacosProperties;
    private final NacosClientFactory nacosClientFactory;
    private final SearchSettings searchSettings;

    public NacosConfigRunner(
            NacosProperties nacosProperties,
            NacosClientFactory nacosClientFactory,
            SearchSettings searchSettings
    ) {
        this.nacosProperties = nacosProperties;
        this.nacosClientFactory = nacosClientFactory;
        this.searchSettings = searchSettings;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!nacosProperties.enabled()) {
            return;
        }
        try {
            ConfigService configService = nacosClientFactory.configService();
            String config = configService.getConfig(nacosProperties.configDataId(), nacosProperties.group(), 3000);
            applyConfig(config);
            configService.addListener(nacosProperties.configDataId(), nacosProperties.group(), new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    applyConfig(configInfo);
                }
            });
            LOGGER.info("Subscribed Nacos config dataId={} group={}", nacosProperties.configDataId(), nacosProperties.group());
        } catch (NacosException e) {
            LOGGER.warn("Failed to read Nacos config dataId={} group={}",
                    nacosProperties.configDataId(), nacosProperties.group(), e);
        }
    }

    void applyConfig(String config) {
        if (config == null || config.isBlank()) {
            return;
        }
        try {
            Properties properties = new Properties();
            properties.load(new StringReader(config));
            String defaultPageSize = properties.getProperty(DEFAULT_PAGE_SIZE_KEY);
            if (defaultPageSize != null) {
                searchSettings.updateDefaultPageSize(Integer.parseInt(defaultPageSize.trim()));
                LOGGER.info("Applied Nacos config {}={}", DEFAULT_PAGE_SIZE_KEY, searchSettings.defaultPageSize());
            }
        } catch (Exception e) {
            LOGGER.warn("Ignored invalid Nacos config content={} reason={}", config, e.getMessage());
        }
    }
}
