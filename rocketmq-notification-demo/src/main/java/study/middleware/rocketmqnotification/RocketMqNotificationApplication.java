package study.middleware.rocketmqnotification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RocketMqNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(RocketMqNotificationApplication.class, args);
    }
}
