package com.raota.agent.application.mobile;

import com.raota.agent.domain.recommendation.model.ShownRecommendation;
import com.raota.agent.domain.recommendation.repository.MobileRecommendationRunRepository;
import com.raota.mobile.common.cursor.Cursor;
import com.raota.mobile.common.cursor.CursorPage;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
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

    MobileRecommendationTickets(
            MobileRecommendationRunRepository runs,
            Clock clock,
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

    /** 오늘 것을 포함해 지금까지 뽑은 식권, 최신순이다. 커서는 마지막 식권의 뽑은 시각과 ID다. */
    CursorPage<ShownRecommendation> history(Long userId, String rawCursor, int size) {
        Instant beforeCreatedAt = null;
        Long beforeId = null;
        Cursor cursor = Cursor.parse(rawCursor).orElse(null);
        if (cursor != null) {
            beforeCreatedAt = parseInstant(cursor);
            beforeId = cursor.id();
        }
        return CursorPage.of(
                runs.findShownBefore(userId, beforeCreatedAt, beforeId, size + 1),
                size,
                run -> Cursor.of(run.createdAt(), run.id()));
    }

    private Instant startOfToday() {
        return LocalDate.now(clock.withZone(SEOUL)).atStartOfDay(SEOUL).toInstant();
    }

    private static Instant parseInstant(Cursor cursor) {
        try {
            return Instant.parse(cursor.sortValue());
        } catch (DateTimeParseException exception) {
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "커서가 올바르지 않습니다.");
        }
    }
}
