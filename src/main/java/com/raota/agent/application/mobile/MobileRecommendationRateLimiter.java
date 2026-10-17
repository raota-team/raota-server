package com.raota.agent.application.mobile;

import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import java.time.Clock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 회원당 1분 구간마다 추천 횟수를 센다. 키에 분 단위 구간을 넣고 매번 만료를 다시 걸어, 만료 설정이 빠져도 키가 남아 회원이 계속 막히지 않는다.
 * Redis 장애 때는 추천을 막지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class MobileRecommendationRateLimiter {

    static final int LIMIT_PER_MINUTE = 10;

    private static final Duration KEY_TTL = Duration.ofMinutes(2);

    private final StringRedisTemplate redis;

    private final Clock clock;

    void acquire(Long userId) {
        String key = "v2:recommendation:rate:" + userId + ":" + clock.millis() / 60_000;
        Long count;
        try {
            count = redis.opsForValue().increment(key);
            redis.expire(key, KEY_TTL);
        }
        catch (RuntimeException exception) {
            log.warn("추천 횟수 제한을 확인하지 못해 허용합니다: userId={}", userId, exception);
            return;
        }
        if (count != null && count > LIMIT_PER_MINUTE) {
            throw new MobileException(MobileErrorCode.RATE_LIMITED, "추천은 1분에 10번까지 받을 수 있습니다.");
        }
    }

}
