package com.raota.mobile.account.infrastructure.auth;

import com.raota.mobile.account.application.port.AccessTokenIssuer;
import com.raota.global.security.jwt.InvalidJwtTokenException;
import com.raota.global.security.jwt.JwtCodec;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Service;

/** 모바일 회원 ID를 v2 발급자와 서명 키로 발급하고 검증한다. */
@Service
public class MobileAccessTokenService implements AccessTokenIssuer {

    private final MobileAuthProperties properties;

    private final JwtCodec codec;

    public MobileAccessTokenService(MobileAuthProperties properties) {
        this.properties = properties;
        this.codec = new JwtCodec(properties.issuer(), properties.accessTokenSecret());
    }

    @Override
    public String issue(Long userId) {
        return codec.issue(String.valueOf(userId), Duration.ofSeconds(properties.accessTokenExpirySeconds()), Map.of());
    }

    @Override
    public long expiresInSeconds() {
        return properties.accessTokenExpirySeconds();
    }

    public Long verify(String token) {
        try {
            return Long.valueOf(codec.verifySubject(token));
        }
        catch (NumberFormatException exception) {
            throw new InvalidJwtTokenException(exception);
        }
    }

}
