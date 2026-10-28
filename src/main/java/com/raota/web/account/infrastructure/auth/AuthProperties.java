package com.raota.web.account.infrastructure.auth;

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
        Cors cors) {
    /**
     * @param allowedRedirectOrigins 로그인 뒤 access token을 실어 보낼 수 있는 출처. CORS 허용 목록과 따로 관리한다. 비우면 항상
     *     {@code redirectUri}·{@code failureRedirectUri}로 보낸다.
     */
    public record OAuth2(String redirectUri, String failureRedirectUri, List<String> allowedRedirectOrigins) {
        public OAuth2 {
            if (allowedRedirectOrigins == null) {
                allowedRedirectOrigins = List.of();
            }
        }
    }

    public record Cookie(String refreshTokenName, boolean secure, String sameSite, String domain) {}

    public record Cors(List<String> allowedOrigins) {}
}
