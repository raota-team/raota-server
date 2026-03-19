package com.raota.global.auth;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String issuer,
        String accessTokenSecret,
        long accessTokenExpirySeconds,
        long refreshTokenExpirySeconds,
        OAuth2 oauth2,
        Cookie cookie,
        Cors cors
) {
    public record OAuth2(String redirectUri, String failureRedirectUri) {
    }

    public record Cookie(String refreshTokenName, boolean secure, String sameSite, String domain) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
