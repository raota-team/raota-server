package com.raota.domain.auth.store;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.auth.refresh-token",
        name = "store-type",
        havingValue = "redis"
)
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String TOKEN_KEY_PREFIX = "auth:refresh:token:";
    private static final String MEMBER_KEY_PREFIX = "auth:refresh:member:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public Optional<StoredRefreshToken> findByToken(String token) {
        String raw = redisTemplate.opsForValue().get(tokenKey(token));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(parse(token, raw));
    }

    @Override
    public Optional<StoredRefreshToken> findByMemberId(Long memberId) {
        String token = redisTemplate.opsForValue().get(memberKey(memberId));
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return findByToken(token);
    }

    @Override
    public void save(Long memberId, String token, Instant expiresAt) {
        findByMemberId(memberId)
                .ifPresent(existing -> redisTemplate.delete(tokenKey(existing.token())));

        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofSeconds(1);
        }

        redisTemplate.opsForValue().set(tokenKey(token), memberId + ":" + expiresAt.toEpochMilli(), ttl);
        redisTemplate.opsForValue().set(memberKey(memberId), token, ttl);
    }

    @Override
    public void deleteByToken(String token) {
        findByToken(token).ifPresent(storedToken ->
                redisTemplate.delete(memberKey(storedToken.memberId()))
        );
        redisTemplate.delete(tokenKey(token));
    }

    private StoredRefreshToken parse(String token, String raw) {
        int delimiterIndex = raw.indexOf(':');
        Long memberId = Long.valueOf(raw.substring(0, delimiterIndex));
        Instant expiresAt = Instant.ofEpochMilli(Long.parseLong(raw.substring(delimiterIndex + 1)));
        return new StoredRefreshToken(memberId, token, expiresAt);
    }

    private String tokenKey(String token) {
        return TOKEN_KEY_PREFIX + token;
    }

    private String memberKey(Long memberId) {
        return MEMBER_KEY_PREFIX + memberId;
    }
}
