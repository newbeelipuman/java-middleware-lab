package study.middleware.rocketmqnotification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NacosConfigRunnerTest {

    @Test
    void appliesSearchDefaultPageSizeFromNacosPropertiesContent() {
        SearchSettings searchSettings = new SearchSettings(10);
        NacosConfigRunner runner = new NacosConfigRunner(
                new NacosProperties(false, "127.0.0.1:8848", "DEFAULT_GROUP", "appointment-notification-service",
                        "rocketmq-notification-demo.properties", "127.0.0.1"),
                null,
                searchSettings
        );

        runner.applyConfig("app.search.default-page-size=2");

        assertThat(searchSettings.defaultPageSize()).isEqualTo(2);
    }

    @Test
    void ignoresInvalidSearchDefaultPageSizeFromNacos() {
        SearchSettings searchSettings = new SearchSettings(10);
        NacosConfigRunner runner = new NacosConfigRunner(
                new NacosProperties(false, "127.0.0.1:8848", "DEFAULT_GROUP", "appointment-notification-service",
                        "rocketmq-notification-demo.properties", "127.0.0.1"),
                null,
                searchSettings
        );

        runner.applyConfig("app.search.default-page-size=200");

        assertThat(searchSettings.defaultPageSize()).isEqualTo(10);
    }
}
