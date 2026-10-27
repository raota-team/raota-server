package com.raota.global.file;

import java.time.Clock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 회원별 업로드 티켓 발급 장수를 1시간 구간마다 센다(웹 v1·앱 v2 공통, #84). 키에 시간 구간을 넣고 매번 만료를 다시 걸어 키가 남아 계속
 * 막히지 않는다. Redis 장애 때는 업로드를 막지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UploadTicketRateLimiter {

    public static final int FILES_PER_HOUR = 30;

    private static final Duration KEY_TTL = Duration.ofHours(2);

    private final StringRedisTemplate redis;

    private final Clock clock;

    /**
     * {@code files}장을 더해도 한도 안이면 true다. {@code scope}는 회원 ID 체계가 다른 웹(v1)과 앱(v2)을 나눈다.
     */
    public boolean tryAcquire(String scope, Long memberId, int files) {
        String key = "upload-ticket:" + scope + ":" + memberId + ":" + clock.millis() / 3_600_000;
        Long count;
        try {
            count = redis.opsForValue().increment(key, files);
            redis.expire(key, KEY_TTL);
        } catch (RuntimeException exception) {
            log.warn("업로드 티켓 한도를 확인하지 못해 허용합니다: scope={}, memberId={}", scope, memberId, exception);
            return true;
        }
        return count == null || count <= FILES_PER_HOUR;
    }
}
