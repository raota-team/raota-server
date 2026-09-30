package com.raota.mobile.account.infrastructure.external;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** 모바일 앱의 소셜 인증 토큰을 검증할 때 사용하는 제공자별 설정이다. */
@ConfigurationProperties("app.mobile.oauth")
@Validated
public record MobileOAuthProperties(@NotNull @Valid Google google, @NotNull @Valid Kakao kakao) {

    public record Google(@NotEmpty List<@NotBlank @Pattern(regexp = "^(?!.*\\$\\{).*$") String> clientIds,
            @DefaultValue("https://www.googleapis.com/oauth2/v3/certs") String jwkSetUri) {
    }

    public record Kakao(@NotBlank @Pattern(regexp = "^(?!.*\\$\\{).*$") String appId,
            @DefaultValue("https://kapi.kakao.com") String apiBaseUrl) {
    }

}
