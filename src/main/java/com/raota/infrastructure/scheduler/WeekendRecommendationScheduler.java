package com.raota.infrastructure.scheduler;

import com.raota.application.member.MemberLifecycleService;
import com.raota.application.recommendation.WeekendCurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeekendRecommendationScheduler {

    private final MemberLifecycleService memberLifecycleService;
    private final WeekendCurationService weekendCurationService;

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void purgeExpiredWithdrawnMembers() {
        int purgedCount = memberLifecycleService.purgeExpiredMembers();
        if (purgedCount > 0) {
            log.info("Finished purging {} withdrawn members", purgedCount);
        }
    }

    @Scheduled(cron = "0 0 6 ? * MON", zone = "Asia/Seoul")
    public void generateWeeklyWeekendRecommendation() {
        log.info("Start weekly weekend ramen recommendation generation");
        weekendCurationService.generateWeeklyCuration();
        log.info("Finish weekly weekend ramen recommendation generation");
    }
}
