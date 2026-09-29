package com.raota.mobile.account.infrastructure.scheduler;

import com.raota.mobile.account.application.service.MobileWithdrawalPurgeService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 탈퇴 유예가 끝난 모바일 회원을 매일 익명화한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MobileAccountPurgeScheduler {

    private final MobileWithdrawalPurgeService purgeService;

    @Scheduled(cron = "0 40 4 * * *", zone = "Asia/Seoul")
    public void purgeExpiredMembers() {
        int purged = purgeService.purgeExpired(Instant.now());
        if (purged > 0) {
            log.info("모바일 탈퇴 회원 {}명 익명화 완료", purged);
        }
    }

}
