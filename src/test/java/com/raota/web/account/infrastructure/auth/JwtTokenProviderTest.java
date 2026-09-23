package com.raota.web.account.infrastructure.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String ACCESS_TOKEN_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void 만료된_액세스_토큰은_전용_인증_예외로_변환한다() {
        String expiredToken = tokenProvider(-60).createAccessToken(1L);

        assertThatThrownBy(() -> tokenProvider(3600).getMemberId(expiredToken))
            .isInstanceOf(ExpiredJwtAuthenticationException.class);
    }

    @Test
    void 형식이_잘못된_액세스_토큰은_일반_인증_예외로_변환한다() {
        assertThatThrownBy(() -> tokenProvider(3600).getMemberId("not-a-jwt"))
            .isInstanceOf(JwtAuthenticationException.class)
            .isNotInstanceOf(ExpiredJwtAuthenticationException.class);
    }

    private JwtTokenProvider tokenProvider(long accessTokenExpirySeconds) {
        AuthProperties authProperties = new AuthProperties("test-issuer", ACCESS_TOKEN_SECRET, accessTokenExpirySeconds,
                1209600, new AuthProperties.OAuth2("/oauth2", "/oauth2/failure"),
                new AuthProperties.Cookie("refreshToken", false, "Lax", null), new AuthProperties.Cors(List.of()));
        return new JwtTokenProvider(authProperties);
    }

}
