package com.raota.global.redis;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * 여러 앱 서버가 공유하는 Redis에서 짧은 임계 구역을 직렬화하는 락.
 *
 * <p>락은 소유자 토큰과 TTL을 가지며, 해제는 토큰이 일치할 때만 수행한다.
 * 업무 데이터의 최종 정합성은 DB 제약으로 보장하고 이 락은 동시 진입을 줄이는 용도로 사용한다.</p>
 */
@Component
public class RedisLockClient {

    private static final RedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisLockClient(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 락을 즉시 한 번 시도한다. 다른 소유자가 보유 중이면 빈 값을 반환한다.
     *
     * @throws RedisLockUnavailableException Redis에 접근할 수 없는 경우
     */
    public Optional<LockToken> tryAcquire(String key, Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("락 TTL은 0보다 커야 합니다.");
        }
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
            return Boolean.TRUE.equals(acquired) ? Optional.of(new LockToken(key, token)) : Optional.empty();
        } catch (DataAccessException exception) {
            throw new RedisLockUnavailableException("Redis 락을 획득할 수 없습니다: " + key, exception);
        }
    }

    /**
     * 토큰이 현재 소유자와 일치할 때만 락을 해제한다.
     *
     * @return 실제로 해제했으면 true
     * @throws RedisLockUnavailableException Redis에 접근할 수 없는 경우
     */
    public boolean release(LockToken lock) {
        try {
            Long deleted = redisTemplate.execute(RELEASE_SCRIPT, List.of(lock.key()), lock.token());
            return deleted != null && deleted > 0;
        } catch (DataAccessException exception) {
            throw new RedisLockUnavailableException("Redis 락을 해제할 수 없습니다: " + lock.key(), exception);
        }
    }

    public record LockToken(String key, String token) {
    }
}
