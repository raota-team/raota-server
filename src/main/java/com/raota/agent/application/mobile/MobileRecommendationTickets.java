package com.raota.agent.application.mobile;

import com.raota.agent.domain.recommendation.model.ShownRecommendation;
import com.raota.agent.domain.recommendation.repository.MobileRecommendationRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 앱 AI 큐레이터의 하루 식권이다. 매장을 하나 이상 보여준 추천 한 번이 한 장이고, 서울 날짜가 바뀌면 다시 채워진다. 노출 기록을 남기지 못한 추천은
 * 세지 못하므로 장수에서 빠진다.
 */
@Component
class MobileRecommendationTickets {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final MobileRecommendationRunRepository runs;

    private final Clock clock;

    private final int dailyLimit;

    MobileRecommendationTickets(MobileRecommendationRunRepository runs, Clock clock,
            @Value("${app.mobile.recommendation.daily-limit:3}") int dailyLimit) {
        this.runs = runs;
        this.clock = clock;
        this.dailyLimit = dailyLimit;
    }

    int dailyLimit() {
        return dailyLimit;
    }

    int remaining(Long userId) {
        return Math.max(0, dailyLimit - runs.countShownSince(userId, startOfToday()));
    }

    /** 오늘 뽑은 식권, 최신순이다. */
    List<ShownRecommendation> today(Long userId) {
        return runs.findShownSince(userId, startOfToday());
    }

    private Instant startOfToday() {
        return LocalDate.now(clock.withZone(SEOUL)).atStartOfDay(SEOUL).toInstant();
    }

}
