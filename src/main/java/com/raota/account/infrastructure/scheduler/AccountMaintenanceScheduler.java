package com.raota.account.infrastructure.scheduler;

import com.raota.account.application.member.MemberLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountMaintenanceScheduler {

    private final MemberLifecycleService memberLifecycleService;

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void purgeExpiredWithdrawnMembers() {
        int purgedCount = memberLifecycleService.purgeExpiredMembers();
        if (purgedCount > 0) {
            log.info("Finished purging {} withdrawn members", purgedCount);
        }
    }
}
