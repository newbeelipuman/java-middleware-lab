package study.middleware.rocketmqnotification;

import java.util.List;

public record NacosStatus(
        boolean enabled,
        boolean registered,
        String serverAddr,
        String serviceName,
        String configDataId,
        int searchDefaultPageSize,
        List<String> serviceBoundaries
) {
}
