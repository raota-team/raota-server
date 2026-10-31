package com.raota.agent.infrastructure.scheduler;

import com.raota.agent.application.recommendation.DailyCurationService;
import com.raota.global.redis.RedisLockClient;
import com.raota.global.redis.RedisLockUnavailableException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 매일 오늘의 큐레이션을 만든다. Redis 락을 잡은 서버 하나만 만들고, Redis에 접근할 수 없으면 락 없이 만든다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyRecommendationScheduler {

    static final String LOCK_KEY = "lock:daily-curation";

    /** 실행이 끝나도 풀지 않고 TTL로만 놓는다. 시계가 조금 늦은 서버가 같은 날 다시 만들지 않는다. */
    private static final Duration LOCK_TTL = Duration.ofHours(1);

    private final DailyCurationService dailyCurationService;

    private final RedisLockClient locks;

    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    public void generateDailyRamenRecommendation() {
        try {
            if (locks.tryAcquire(LOCK_KEY, LOCK_TTL).isEmpty()) {
                log.info("오늘의 큐레이션: 다른 서버가 만들고 있어 건너뜀");
                return;
            }
        } catch (RedisLockUnavailableException exception) {
            log.warn("오늘의 큐레이션: Redis 락을 잡지 못해 락 없이 만든다", exception);
        }
        log.info("Start daily ramen recommendation generation");
        dailyCurationService.generateDailyCuration();
        log.info("Finish daily ramen recommendation generation");
    }
}
