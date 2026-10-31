package com.raota.mobile.account.infrastructure.scheduler;

import com.raota.global.redis.RedisLockClient;
import com.raota.global.redis.RedisLockUnavailableException;
import com.raota.mobile.account.application.service.MobileWithdrawalPurgeService;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 탈퇴 유예가 끝난 모바일 회원을 매일 익명화한다. Redis 락을 잡은 서버 하나만 실행하고, 락을 못 잡으면 건너뛴다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MobileAccountPurgeScheduler {

    static final String LOCK_KEY = "lock:v2:account-purge";

    /** 실행이 끝나도 풀지 않고 TTL로만 놓는다. 시계가 조금 늦은 서버가 같은 날 다시 돌지 않는다. */
    private static final Duration LOCK_TTL = Duration.ofHours(1);

    private final MobileWithdrawalPurgeService purgeService;

    private final RedisLockClient locks;

    @Scheduled(cron = "0 40 4 * * *", zone = "Asia/Seoul")
    public void purgeExpiredMembers() {
        try {
            if (locks.tryAcquire(LOCK_KEY, LOCK_TTL).isEmpty()) {
                log.info("모바일 탈퇴 회원 익명화: 다른 서버가 실행 중이라 건너뜀");
                return;
            }
        } catch (RedisLockUnavailableException exception) {
            log.warn("모바일 탈퇴 회원 익명화: Redis 락을 잡지 못해 건너뜀", exception);
            return;
        }
        int purged = purgeService.purgeExpired(Instant.now());
        if (purged > 0) {
            log.info("모바일 탈퇴 회원 {}명 익명화 완료", purged);
        }
    }
}
