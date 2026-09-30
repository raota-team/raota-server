package com.raota.mobile.account.infrastructure.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class MobileAuthPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void 시크릿이_없으면_애플리케이션_컨텍스트가_시작되지_않는다() {
        contextRunner.withPropertyValues("app.mobile.auth.issuer=raota-mobile-test")
            .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    @Test
    void 시크릿_환경변수가_없어_placeholder가_그대로_남으면_애플리케이션_컨텍스트가_시작되지_않는다() {
        contextRunner
            .withPropertyValues("app.mobile.auth.issuer=raota-mobile-test",
                    "app.mobile.auth.access-token-secret=${APP_MOBILE_AUTH_ACCESS_TOKEN_SECRET_FOR_TEST_ONLY}")
            .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    @Test
    void 만료_설정이_없으면_기본값_1800초를_사용한다() {
        contextRunner
            .withPropertyValues("app.mobile.auth.issuer=raota-mobile-test",
                    "app.mobile.auth.access-token-secret=mobile-v2-signing-secret-for-tests-0123456789abcdef")
            .run(context -> assertThat(context.getBean(MobileAuthProperties.class).accessTokenExpirySeconds())
                .isEqualTo(1800));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MobileAuthProperties.class)
    static class PropertiesConfiguration {

    }

}
