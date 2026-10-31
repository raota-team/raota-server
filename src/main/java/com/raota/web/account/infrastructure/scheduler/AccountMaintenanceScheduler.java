package com.raota.web.account.infrastructure.scheduler;

import com.raota.global.redis.RedisLockClient;
import com.raota.global.redis.RedisLockUnavailableException;
import com.raota.web.account.application.member.MemberLifecycleService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 탈퇴 유예가 끝난 웹 회원을 매일 정리한다. Redis 락을 잡은 서버 하나만 실행하고, 락을 못 잡으면 건너뛴다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountMaintenanceScheduler {

    static final String LOCK_KEY = "lock:v1:account-purge";

    /** 실행이 끝나도 풀지 않고 TTL로만 놓는다. 시계가 조금 늦은 서버가 같은 날 다시 돌지 않는다. */
    private static final Duration LOCK_TTL = Duration.ofHours(1);

    private final MemberLifecycleService memberLifecycleService;

    private final RedisLockClient locks;

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void purgeExpiredWithdrawnMembers() {
        try {
            if (locks.tryAcquire(LOCK_KEY, LOCK_TTL).isEmpty()) {
                log.info("Skip purging withdrawn members: another server is running it");
                return;
            }
        } catch (RedisLockUnavailableException exception) {
            log.warn("Skip purging withdrawn members: Redis lock unavailable", exception);
            return;
        }
        int purgedCount = memberLifecycleService.purgeExpiredMembers();
        if (purgedCount > 0) {
            log.info("Finished purging {} withdrawn members", purgedCount);
        }
    }
}
