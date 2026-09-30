package com.raota.mobile.account.infrastructure.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 모바일 v2 access token 설정이다. 서명 키가 없거나 {@code ${...}}가 해석되지 않고 남아 있으면 애플리케이션을 시작하지 않는다.
 */
@ConfigurationProperties("app.mobile.auth")
@Validated
public record MobileAuthProperties(@NotBlank String issuer,
        @NotBlank @Pattern(regexp = "^(?!.*\\$\\{).*$",
                message = "해석되지 않은 placeholder는 서명 키로 쓸 수 없습니다.") String accessTokenSecret,
        @DefaultValue("1800") long accessTokenExpirySeconds) {
}
