package com.raota.web.account.infrastructure.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.raota.global.security.jwt.InvalidJwtTokenException;
import com.raota.global.security.jwt.JwtCodec;
import com.raota.global.security.jwt.ExpiredJwtTokenException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String ACCESS_TOKEN_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void 만료된_액세스_토큰은_코덱_원인을_유지한_인증_예외로_변환한다() {
        String expiredToken = tokenProvider(-60).createAccessToken(1L);

        assertThatThrownBy(() -> tokenProvider(3600).getMemberId(expiredToken))
            .isInstanceOf(JwtAuthenticationException.class)
            .hasCauseInstanceOf(ExpiredJwtTokenException.class);
    }

    @Test
    void 형식이_잘못된_액세스_토큰은_일반_인증_예외로_변환한다() {
        assertThatThrownBy(() -> tokenProvider(3600).getMemberId("not-a-jwt"))
            .isInstanceOf(JwtAuthenticationException.class);
    }

    @Test
    void 발급한_v1_토큰은_기존_발급자와_회원_ID_클레임을_보존한다() {
        String token = tokenProvider(3600).createAccessToken(42L);
        var claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(ACCESS_TOKEN_SECRET)))
            .build()
            .parseSignedClaims(token)
            .getPayload();

        assertThat(claims.getIssuer()).isEqualTo("test-issuer");
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(((Number) claims.get("memberId")).longValue()).isEqualTo(42L);
        assertThat(claims.getIssuedAt()).isBefore(claims.getExpiration());
    }

    @Test
    void 같은_키라도_발급자가_다른_토큰은_v1에서_거절한다() {
        String token = new JwtCodec("another-issuer", ACCESS_TOKEN_SECRET).issue("42", Duration.ofMinutes(30),
                Map.of("memberId", 42L));

        assertThatThrownBy(() -> tokenProvider(3600).getMemberId(token)).isInstanceOf(JwtAuthenticationException.class)
            .hasCauseInstanceOf(InvalidJwtTokenException.class);
    }

    private JwtTokenProvider tokenProvider(long accessTokenExpirySeconds) {
        AuthProperties authProperties = new AuthProperties("test-issuer", ACCESS_TOKEN_SECRET, accessTokenExpirySeconds,
                1209600, new AuthProperties.OAuth2("/oauth2", "/oauth2/failure"),
                new AuthProperties.Cookie("refreshToken", false, "Lax", null), new AuthProperties.Cors(List.of()));
        return new JwtTokenProvider(authProperties);
    }

}
