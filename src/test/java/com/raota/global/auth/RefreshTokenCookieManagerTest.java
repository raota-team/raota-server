package com.raota.global.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshTokenCookieManagerTest {

    @Test
    void createRefreshTokenCookieUsesCrossSiteCookieAttributes() {
        RefreshTokenCookieManager cookieManager = new RefreshTokenCookieManager(authProperties());

        ResponseCookie cookie = cookieManager.createRefreshTokenCookie("refresh-token");

        assertThat(cookie.toString())
                .contains("raota_refresh_token=refresh-token")
                .contains("Path=/")
                .contains("Domain=.raota.net")
                .contains("Max-Age=1209600")
                .contains("Expires=")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=None")
                .doesNotContain("SameSite='None'");
    }

    @Test
    void clearRefreshTokenCookieUsesSameScopeAsCreateCookie() {
        RefreshTokenCookieManager cookieManager = new RefreshTokenCookieManager(authProperties());

        ResponseCookie cookie = cookieManager.clearRefreshTokenCookie();

        assertThat(cookie.toString())
                .contains("raota_refresh_token=")
                .contains("Path=/")
                .contains("Domain=.raota.net")
                .contains("Max-Age=0")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=None");
    }

    private AuthProperties authProperties() {
        return new AuthProperties(
                "raota",
                "access-token-secret",
                1800,
                1209600,
                new AuthProperties.OAuth2(
                        "http://localhost:3000/auth/callback",
                        "http://localhost:3000/auth/callback"
                ),
                new AuthProperties.Cookie(
                        "raota_refresh_token",
                        true,
                        "None",
                        ".raota.net"
                ),
                new AuthProperties.Cors(List.of("http://localhost:3000"))
        );
    }
}
