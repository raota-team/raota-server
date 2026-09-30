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
        redis.opsForValue()
            .set(key(token), userId.toString(), Duration.ofSeconds(properties.refreshTokenExpirySeconds()));
        return token;
    }

    @Override
    public Optional<Long> consume(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }
        String key = key(refreshToken);
        String value = redis.opsForValue().get(key);
        if (value == null || !Boolean.TRUE.equals(redis.delete(key))) {
            return Optional.empty();
        }
        return Optional.of(Long.valueOf(value));
    }

    @Override
    public void revoke(String refreshToken, Long userId) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        String key = key(refreshToken);
        if (userId.toString().equals(redis.opsForValue().get(key))) {
            redis.delete(key);
        }
    }

    private String key(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return properties.refreshTokenKeyPrefix() + HexFormat.of().formatHex(hash);
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }

}
