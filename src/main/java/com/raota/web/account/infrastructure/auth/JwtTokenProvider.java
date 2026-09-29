package com.raota.web.account.infrastructure.auth;

import com.raota.global.security.jwt.ExpiredJwtTokenException;
import com.raota.global.security.jwt.JwtCodec;
import com.raota.global.security.jwt.JwtTokenException;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final AuthProperties authProperties;

    private final JwtCodec codec;

    public JwtTokenProvider(AuthProperties authProperties) {
        this.authProperties = authProperties;
        this.codec = new JwtCodec(authProperties.issuer(), authProperties.accessTokenSecret());
    }

    public String createAccessToken(Long memberId) {
        return codec.issue(String.valueOf(memberId), Duration.ofSeconds(authProperties.accessTokenExpirySeconds()),
                Map.of("memberId", memberId));
    }

    public Long getMemberId(String token) {
        try {
            return Long.valueOf(codec.verifySubject(token));
        }
        catch (ExpiredJwtTokenException exception) {
            throw new ExpiredJwtAuthenticationException(exception);
        }
        catch (JwtTokenException | NumberFormatException exception) {
            throw new JwtAuthenticationException("유효하지 않은 액세스 토큰입니다.", exception);
        }
    }

    public long accessTokenExpirySeconds() {
        return authProperties.accessTokenExpirySeconds();
    }

    public long refreshTokenExpirySeconds() {
        return authProperties.refreshTokenExpirySeconds();
    }

}
