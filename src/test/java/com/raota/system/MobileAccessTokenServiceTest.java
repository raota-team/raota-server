package com.raota.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.raota.global.security.jwt.ExpiredJwtTokenException;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.mobile.account.infrastructure.auth.MobileAuthProperties;
import com.raota.global.security.jwt.InvalidJwtTokenException;
import com.raota.web.account.infrastructure.auth.AuthProperties;
import com.raota.web.account.infrastructure.auth.JwtTokenProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class MobileAccessTokenServiceTest {

    private static final String MOBILE_SECRET = "mobile-v2-signing-secret-for-tests-0123456789abcdef";

    @Test
    void 발급한_모바일_토큰에서_회원_ID를_검증한다() {
        var service = mobileTokens(1800);

        assertThat(service.verify(service.issue(42L))).isEqualTo(42L);
    }

    @Test
    void 기존_웹_토큰은_모바일_토큰으로_사용할_수_없다() {
        var v1Properties = new AuthProperties("raota-test", "test-raota-access-token-secret-key-1234567890", 1800,
                1209600, new AuthProperties.OAuth2("/oauth2", "/oauth2/failure"),
                new AuthProperties.Cookie("refreshToken", false, "Lax", null), new AuthProperties.Cors(List.of()));
        String webToken = new JwtTokenProvider(v1Properties).createAccessToken(42L);

        assertThatThrownBy(() -> mobileTokens(1800).verify(webToken)).isInstanceOf(InvalidJwtTokenException.class);
    }

    @Test
    void 만료된_모바일_토큰은_만료_예외로_구분한다() {
        String expiredToken = mobileTokens(-60).issue(42L);

        assertThatThrownBy(() -> mobileTokens(1800).verify(expiredToken)).isInstanceOf(ExpiredJwtTokenException.class);
    }

    private MobileAccessTokenService mobileTokens(long expirySeconds) {
        return new MobileAccessTokenService(
                new MobileAuthProperties("raota-mobile-test", MOBILE_SECRET, expirySeconds, 1209600, "v2:refresh:"));
    }

}
