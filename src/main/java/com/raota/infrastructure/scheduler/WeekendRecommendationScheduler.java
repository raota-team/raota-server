package com.raota.infrastructure.scheduler;

import com.raota.application.recommendation.WeekendCurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeekendRecommendationScheduler {

    private final WeekendCurationService weekendCurationService;

    @Scheduled(cron = "0 0 6 ? * MON", zone = "Asia/Seoul")
    public void generateWeeklyWeekendRecommendation() {
        log.info("Start weekly weekend ramen recommendation generation");
        weekendCurationService.generateWeeklyCuration();
        log.info("Finish weekly weekend ramen recommendation generation");
    }
}
