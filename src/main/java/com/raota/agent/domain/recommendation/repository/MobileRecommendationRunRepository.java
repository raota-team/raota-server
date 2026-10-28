package com.raota.agent.domain.recommendation.repository;

import com.raota.agent.domain.recommendation.model.MobileRecommendationRun;
import com.raota.agent.domain.recommendation.model.RecommendationImpression;
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

    /**
     * 이 회원에게 매장을 하나 이상 보여준 추천을 {@code created_at DESC, id DESC}로 최대 {@code limit}개 돌려준다. 항목은 보여준
     * 순서다. {@code beforeCreatedAt}·{@code beforeId}가 있으면 그 기록보다 뒤(더 오래된 것)만 읽고, 없으면 처음부터 읽는다.
     */
    List<ShownRecommendation> findShownBefore(Long userId, Instant beforeCreatedAt, Long beforeId, int limit);

    /**
     * 이 회원의 추천 {@code runId}에 보여준 매장이면 처음 연 시각을 남긴다. 이미 열었으면 처음 시각을 그대로 둔다. 그런 항목이 없으면
     * {@code false}다.
     */
    boolean markOpened(Long userId, Long runId, Long shopId, Instant openedAt);

    /** {@code [from, to)} 사이 추천에서 보여준 매장 칸 전부다. */
    List<RecommendationImpression> findImpressions(Instant from, Instant to);
}
