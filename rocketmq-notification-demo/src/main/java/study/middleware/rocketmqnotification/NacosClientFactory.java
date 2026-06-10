package study.middleware.rocketmqnotification;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.PropertyKeyConst;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class NacosClientFactory {

    private final NacosProperties nacosProperties;

    public NacosClientFactory(NacosProperties nacosProperties) {
        this.nacosProperties = nacosProperties;
    }

    public NamingService namingService() throws NacosException {
        return NacosFactory.createNamingService(properties());
    }

    public ConfigService configService() throws NacosException {
        return NacosFactory.createConfigService(properties());
    }

    private Properties properties() {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, nacosProperties.serverAddr());
        return properties;
    }
}
