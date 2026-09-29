package com.raota.mobile.account.infrastructure.auth;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("app.mobile.auth")
@Validated
public record MobileAuthProperties(@NotBlank String issuer, @NotBlank String accessTokenSecret,
        @DefaultValue("1800") long accessTokenExpirySeconds) {
}
