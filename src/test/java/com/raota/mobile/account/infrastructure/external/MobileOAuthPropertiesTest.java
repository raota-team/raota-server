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
        contextRunner.withPropertyValues(apple())
            .withPropertyValues("app.mobile.oauth.kakao.app-id=123456")
            .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    @Test
    void Kakao_앱_ID가_없으면_시작하지_않는다() {
        contextRunner.withPropertyValues(apple())
            .withPropertyValues("app.mobile.oauth.google.client-ids=mobile-client")
            .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    @Test
    void Google_클라이언트_ID에_미해결_placeholder가_있으면_시작하지_않는다() {
        contextRunner.withPropertyValues(apple())
            .withPropertyValues("app.mobile.oauth.google.client-ids[0]=mobile-client",
                    "app.mobile.oauth.google.client-ids[1]=${MISSING_MOBILE_GOOGLE_CLIENT_ID}",
                    "app.mobile.oauth.kakao.app-id=123456")
            .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    @Test
    void Kakao_앱_ID에_미해결_placeholder가_있으면_시작하지_않는다() {
        contextRunner.withPropertyValues(apple())
            .withPropertyValues("app.mobile.oauth.google.client-ids=mobile-client",
                    "app.mobile.oauth.kakao.app-id=${MISSING_MOBILE_KAKAO_APP_ID}")
            .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    @Test
    void Google_클라이언트_ID_목록에_빈_항목이_있으면_시작하지_않는다() {
        contextRunner.withPropertyValues(apple())
            .withPropertyValues("app.mobile.oauth.google.client-ids[0]=mobile-client",
                    "app.mobile.oauth.google.client-ids[1]= ", "app.mobile.oauth.kakao.app-id=123456")
            .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    @Test
    void 쉼표로_구분한_클라이언트_ID와_기본_API_주소를_읽는다() {
        contextRunner.withPropertyValues(apple())
            .withPropertyValues("app.mobile.oauth.google.client-ids=first-client,second-client",
                    "app.mobile.oauth.kakao.app-id=123456")
            .run(context -> {
                assertThat(context).hasNotFailed();
                var properties = context.getBean(MobileOAuthProperties.class);
                assertThat(properties.google().clientIds()).isEqualTo(List.of("first-client", "second-client"));
                assertThat(properties.google().jwkSetUri()).isEqualTo("https://www.googleapis.com/oauth2/v3/certs");
                assertThat(properties.kakao().apiBaseUrl()).isEqualTo("https://kapi.kakao.com");
                assertThat(properties.apple().issuer()).isEqualTo("https://appleid.apple.com");
            });
    }

    @Test
    void Apple의_필수_설정이_없거나_잘못된_값이면_시작하지_않는다() {
        String[] names = { "bundle-id", "team-id", "key-id", "private-key", "token-encryption-key" };
        for (String name : names) {
            contextRunner
                .withPropertyValues("app.mobile.oauth.google.client-ids=mobile-client",
                        "app.mobile.oauth.kakao.app-id=123456")
                .withPropertyValues(java.util.Arrays.stream(apple())
                    .filter(property -> !property.startsWith("app.mobile.oauth.apple." + name + "="))
                    .toArray(String[]::new))
                .run(context -> assertThat(context.getStartupFailure()).as(name).isNotNull());
            contextRunner.withPropertyValues(apple())
                .withPropertyValues("app.mobile.oauth.google.client-ids=mobile-client",
                        "app.mobile.oauth.kakao.app-id=123456", "app.mobile.oauth.apple." + name + "=${MISSING_APPLE}")
                .run(context -> assertThat(context.getStartupFailure()).as(name).isNotNull());
        }
        for (String bad : new String[] { "short", "lowercase1" }) {
            contextRunner.withPropertyValues(apple())
                .withPropertyValues("app.mobile.oauth.google.client-ids=mobile-client",
                        "app.mobile.oauth.kakao.app-id=123456", "app.mobile.oauth.apple.team-id=" + bad)
                .run(context -> assertThat(context.getStartupFailure()).isNotNull());
        }
        contextRunner.withPropertyValues(apple())
            .withPropertyValues("app.mobile.oauth.google.client-ids=mobile-client",
                    "app.mobile.oauth.kakao.app-id=123456", "app.mobile.oauth.apple.token-encryption-key=YWJj")
            .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    private static String[] apple() {
        return new String[] { "app.mobile.oauth.apple.bundle-id=net.raota.mobile.test",
                "app.mobile.oauth.apple.team-id=TESTTEAM01", "app.mobile.oauth.apple.key-id=TESTKEY001",
                "app.mobile.oauth.apple.private-key=test-key",
                "app.mobile.oauth.apple.token-encryption-key=cm2PatRyO0rn/bD+j2wAupkmWGwmDGXdnq3PxZx59Fk=" };
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MobileOAuthProperties.class)
    static class PropertiesConfiguration {

    }

}
