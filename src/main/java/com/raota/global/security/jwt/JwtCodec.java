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

    public JwtCodec(String issuer, String secret) {
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
