package com.raota.agent.application.mobile;

import com.raota.agent.domain.recommendation.repository.MobileRecommendationRunRepository;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 회원이 추천 결과에서 매장을 연 것을 노출 기록에 남긴다. 성과 집계의 열기율이 된다. */
@Service
@RequiredArgsConstructor
public class MobileRecommendationFeedbackService {

    private final MobileRecommendationRunRepository runs;

    private final Clock clock;

    /** 처음 연 시각만 남기므로 여러 번 불러도 결과가 같다. 내 추천이 아니거나 그 추천에 없는 매장이면 404다. */
    public void markOpened(Long userId, Long recommendationId, Long shopId) {
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        if (!runs.markOpened(userId, recommendationId, shopId, now)) {
            throw new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "추천 기록을 찾을 수 없습니다.");
        }
    }
}
