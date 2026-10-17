package com.raota.agent.domain.recommendation.repository;

import com.raota.agent.domain.recommendation.model.MobileRecommendationRun;

public interface MobileRecommendationRunRepository {

    /** 기록과 항목을 함께 저장하고 기록 ID를 돌려준다. */
    Long save(MobileRecommendationRun run);

    void deleteByUserId(Long userId);

}
