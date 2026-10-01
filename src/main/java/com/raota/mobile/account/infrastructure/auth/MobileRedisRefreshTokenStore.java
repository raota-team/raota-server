package com.raota.mobile.account.infrastructure.auth;

import com.raota.mobile.account.application.port.RefreshTokenStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** 토큰 자체는 저장하지 않고 SHA-256 해시로 Redis 키를 만든다. */
@Component
public class MobileRedisRefreshTokenStore implements RefreshTokenStore {

    private final StringRedisTemplate redis;

    private final MobileAuthProperties properties;

    private final SecureRandom random = new SecureRandom();

    public MobileRedisRefreshTokenStore(StringRedisTemplate redis, MobileAuthProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public String issue(Long userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash = hash(token);
        Duration lifetime = Duration.ofSeconds(properties.refreshTokenExpirySeconds());
        redis.opsForValue().set(properties.refreshTokenKeyPrefix() + hash, userId.toString(), lifetime);
        String userKey = userKey(userId);
        redis.opsForSet().add(userKey, hash);
        redis.expire(userKey, lifetime);
        return token;
    }

    @Override
    public Optional<Long> consume(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }
        String hash = hash(refreshToken);
        String value = redis.opsForValue().getAndDelete(properties.refreshTokenKeyPrefix() + hash);
        if (value == null) {
            return Optional.empty();
        }
        Long userId = Long.valueOf(value);
        redis.opsForSet().remove(userKey(userId), hash);
        return Optional.of(userId);
    }

    @Override
    public void revoke(String refreshToken, Long userId) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        String hash = hash(refreshToken);
        String tokenKey = properties.refreshTokenKeyPrefix() + hash;
        if (userId.toString().equals(redis.opsForValue().get(tokenKey))) {
            redis.delete(tokenKey);
            redis.opsForSet().remove(userKey(userId), hash);
        }
    }

    @Override
    public void revokeAll(Long userId) {
        String userKey = userKey(userId);
        Set<String> hashes = redis.opsForSet().members(userKey);
        if (hashes != null && !hashes.isEmpty()) {
            redis.delete(hashes.stream().map(hash -> properties.refreshTokenKeyPrefix() + hash).toList());
        }
        redis.delete(userKey);
    }

    private String userKey(Long userId) {
        return properties.refreshTokenKeyPrefix() + "user:" + userId;
    }

    private String hash(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }

}
