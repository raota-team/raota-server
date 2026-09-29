package com.raota.mobile.account.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class MobileOAuthPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void Google_클라이언트_ID가_없으면_시작하지_않는다() {
        contextRunner.withPropertyValues("app.mobile.oauth.kakao.app-id=123456")
            .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    @Test
    void Kakao_앱_ID가_없으면_시작하지_않는다() {
        contextRunner.withPropertyValues("app.mobile.oauth.google.client-ids=mobile-client")
            .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    @Test
    void Google_클라이언트_ID에_미해결_placeholder가_있으면_시작하지_않는다() {
        contextRunner
            .withPropertyValues("app.mobile.oauth.google.client-ids[0]=mobile-client",
                    "app.mobile.oauth.google.client-ids[1]=${MISSING_MOBILE_GOOGLE_CLIENT_ID}",
                    "app.mobile.oauth.kakao.app-id=123456")
            .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    @Test
    void Kakao_앱_ID에_미해결_placeholder가_있으면_시작하지_않는다() {
        contextRunner
            .withPropertyValues("app.mobile.oauth.google.client-ids=mobile-client",
                    "app.mobile.oauth.kakao.app-id=${MISSING_MOBILE_KAKAO_APP_ID}")
            .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    @Test
    void 쉼표로_구분한_클라이언트_ID와_기본_API_주소를_읽는다() {
        contextRunner
            .withPropertyValues("app.mobile.oauth.google.client-ids=first-client,second-client",
                    "app.mobile.oauth.kakao.app-id=123456")
            .run(context -> {
                assertThat(context).hasNotFailed();
                var properties = context.getBean(MobileOAuthProperties.class);
                assertThat(properties.google().clientIds()).isEqualTo(List.of("first-client", "second-client"));
                assertThat(properties.google().jwkSetUri()).isEqualTo("https://www.googleapis.com/oauth2/v3/certs");
                assertThat(properties.kakao().apiBaseUrl()).isEqualTo("https://kapi.kakao.com");
            });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MobileOAuthProperties.class)
    static class PropertiesConfiguration {

    }

}
