package com.raota.mobile.account.infrastructure.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.mobile.account.application.port.RefreshTokenStore;
import com.raota.support.BaseIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisRefreshTokenStoreIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RefreshTokenStore tokens;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private MobileAuthProperties properties;

    @Test
    void 토큰은_해시_키에_만료_시간과_함께_저장되고_한_번만_사용할_수_있다() throws Exception {
        String token = tokens.issue(42L);
        String key = key(token);
        try {
            assertThat(redis.hasKey(properties.refreshTokenKeyPrefix() + token)).isFalse();
            assertThat(redis.opsForValue().get(key)).isEqualTo("42");
            assertThat(redis.getExpire(key, TimeUnit.SECONDS)).isBetween(1L, properties.refreshTokenExpirySeconds());
            assertThat(tokens.consume(token)).contains(42L);
            assertThat(tokens.consume(token)).isEmpty();
            assertThat(redis.hasKey(key)).isFalse();
        }
        finally {
            redis.delete(key);
        }
    }

    @Test
    void 다른_회원은_토큰을_폐기할_수_없으며_기기마다_토큰이_별도로_유지된다() throws Exception {
        String deviceA = tokens.issue(42L);
        String deviceB = tokens.issue(42L);
        try {
            assertThat(deviceA).isNotEqualTo(deviceB);
            tokens.revoke(deviceA, 43L);
            assertThat(tokens.consume(deviceA)).contains(42L);
            tokens.revoke(deviceB, 42L);
            assertThat(tokens.consume(deviceB)).isEmpty();
        }
        finally {
            redis.delete(key(deviceA));
            redis.delete(key(deviceB));
        }
    }

    @Test
    void 동시_재사용은_한_요청만_성공한다() throws Exception {
        String token = tokens.issue(42L);
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            var first = executor.submit(() -> {
                start.await();
                return tokens.consume(token);
            });
            var second = executor.submit(() -> {
                start.await();
                return tokens.consume(token);
            });
            start.countDown();
            assertThat(first.get().stream().count() + second.get().stream().count()).isEqualTo(1);
        }
        finally {
            redis.delete(key(token));
        }
    }

    private String key(String token) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        return properties.refreshTokenKeyPrefix() + HexFormat.of().formatHex(hash);
    }

}
