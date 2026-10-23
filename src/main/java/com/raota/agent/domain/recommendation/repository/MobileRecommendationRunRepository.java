package com.raota.agent.domain.recommendation.repository;

import com.raota.agent.domain.recommendation.model.MobileRecommendationRun;
import com.raota.agent.domain.recommendation.model.ShownRecommendation;
import java.time.Instant;
import java.util.List;

public interface MobileRecommendationRunRepository {

    /** 기록과 항목을 함께 저장하고 기록 ID를 돌려준다. */
    Long save(MobileRecommendationRun run);

    void deleteByUserId(Long userId);

    /** {@code since} 이후 이 회원에게 매장을 하나 이상 보여준 추천 수다. */
    int countShownSince(Long userId, Instant since);

    /** {@code since} 이후 이 회원에게 매장을 하나 이상 보여준 추천을 최신순으로, 항목은 보여준 순서로 돌려준다. */
    List<ShownRecommendation> findShownSince(Long userId, Instant since);

}
