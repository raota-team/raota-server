package com.raota.mobile.account.application.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class MobileAccountPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void 기본_기간은_30일이고_양수_기간으로_변경할_수_있다() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(MobileAccountProperties.class).withdrawalGracePeriod())
                .isEqualTo(Duration.ofDays(30));
        });
        runner.withPropertyValues("app.mobile.account.withdrawal-grace-period=48h").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(MobileAccountProperties.class).withdrawalGracePeriod())
                .isEqualTo(Duration.ofHours(48));
        });
    }

    @Test
    void 유예_기간이_0이거나_음수면_시작하지_않는다() {
        for (String invalid : new String[] { "0s", "-1d" }) {
            runner.withPropertyValues("app.mobile.account.withdrawal-grace-period=" + invalid)
                .run(context -> assertThat(context.getStartupFailure()).as(invalid).isNotNull());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MobileAccountProperties.class)
    static class PropertiesConfiguration {

    }

}
