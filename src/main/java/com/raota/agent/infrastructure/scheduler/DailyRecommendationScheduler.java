package com.raota.agent.infrastructure.scheduler;

import com.raota.agent.application.recommendation.DailyCurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyRecommendationScheduler {

    private final DailyCurationService dailyCurationService;

    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    public void generateDailyRamenRecommendation() {
        log.info("Start daily ramen recommendation generation");
        dailyCurationService.generateDailyCuration();
        log.info("Finish daily ramen recommendation generation");
    }
}
