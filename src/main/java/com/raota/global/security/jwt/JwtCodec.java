package com.raota.global.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;

/** 발급자와 서명 키를 함께 검증하는 액세스 토큰 코덱이다. */
public class JwtCodec {

    private final String issuer;

    private final SecretKey signingKey;

    /**
     * 서명 키가 비어 있거나 {@code ${...}} placeholder가 해석되지 않은 채 남아 있으면 거부한다. 설정 바인딩은 해석하지 못한
     * placeholder를 문자 그대로 넘기므로, 그대로 쓰면 누구나 아는 값이 서명 키가 된다.
     */
    public JwtCodec(String issuer, String secret) {
        if (secret == null || secret.isBlank() || secret.contains("${")) {
            throw new IllegalArgumentException("JWT 서명 키가 설정되지 않았습니다.");
        }
        this.issuer = issuer;
        this.signingKey = createSigningKey(secret);
    }

    public String issue(String subject, Duration ttl, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(issuer)
            .subject(subject)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(ttl)))
            .claims(claims)
            .signWith(signingKey)
            .compact();
    }

    public String verifySubject(String token) {
        try {
            return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        }
        catch (ExpiredJwtException exception) {
            throw new ExpiredJwtTokenException(exception);
        }
        catch (RuntimeException exception) {
            throw new InvalidJwtTokenException(exception);
        }
    }

    private static SecretKey createSigningKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        }
        catch (RuntimeException exception) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
