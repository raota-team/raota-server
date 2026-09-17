package com.raota.global.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.raota.global.redis.RedisLockClient.LockToken;
import com.raota.support.BaseIntegrationTest;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisLockClientIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RedisLockClient lockClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("보유 중인 락은 다른 소유자가 획득할 수 없다")
    void lockIsExclusive() {
        String key = key();

        Optional<LockToken> first = lockClient.tryAcquire(key, Duration.ofSeconds(30));
        Optional<LockToken> second = lockClient.tryAcquire(key, Duration.ofSeconds(30));

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
    }

    @Test
    @DisplayName("소유자 토큰이 다르면 락을 해제하지 못한다")
    void onlyOwnerCanRelease() {
        String key = key();
        LockToken owner = lockClient.tryAcquire(key, Duration.ofSeconds(30)).orElseThrow();

        boolean releasedByOther = lockClient.release(new LockToken(key, "other-token"));

        assertThat(releasedByOther).isFalse();
        assertThat(redisTemplate.opsForValue().get(key)).isEqualTo(owner.token());
        assertThat(lockClient.release(owner)).isTrue();
        assertThat(redisTemplate.hasKey(key)).isFalse();
    }

    @Test
    @DisplayName("만료된 이전 소유자는 새 소유자의 락을 해제하지 못한다")
    void expiredOwnerCannotReleaseNewOwnersLock() {
        String key = key();
        LockToken expired = lockClient.tryAcquire(key, Duration.ofMillis(200)).orElseThrow();

        Awaitility.await().atMost(Duration.ofSeconds(3))
                .until(() -> lockClient.tryAcquire(key, Duration.ofSeconds(30)).isPresent());

        assertThat(lockClient.release(expired)).isFalse();
        assertThat(redisTemplate.hasKey(key)).isTrue();
    }

    @Test
    @DisplayName("TTL은 0보다 커야 한다")
    void rejectsNonPositiveTtl() {
        assertThatThrownBy(() -> lockClient.tryAcquire(key(), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static String key() {
        return "test:lock:" + UUID.randomUUID();
    }
}
