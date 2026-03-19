package com.raota.global.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final AuthProperties authProperties;
    private final SecretKey signingKey;

    public JwtTokenProvider(AuthProperties authProperties) {
        this.authProperties = authProperties;
        this.signingKey = createSigningKey(authProperties.accessTokenSecret());
    }

    public String createAccessToken(Long memberId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(authProperties.accessTokenExpirySeconds());
        return Jwts.builder()
                .issuer(authProperties.issuer())
                .subject(String.valueOf(memberId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("memberId", memberId)
                .signWith(signingKey)
                .compact();
    }

    public AuthenticatedMember getAuthenticatedMember(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return AuthenticatedMember.user(Long.valueOf(claims.getSubject()));
        } catch (RuntimeException exception) {
            throw new JwtAuthenticationException("유효하지 않은 액세스 토큰입니다.", exception);
        }
    }

    public long accessTokenExpirySeconds() {
        return authProperties.accessTokenExpirySeconds();
    }

    public long refreshTokenExpirySeconds() {
        return authProperties.refreshTokenExpirySeconds();
    }

    private SecretKey createSigningKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException exception) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
