package com.raota.agent.infrastructure.messaging;

import com.raota.agent.domain.recommendation.repository.MobileRecommendationRunRepository;
import com.raota.mobile.account.domain.event.MobileMemberPurgedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 회원 익명화 트랜잭션 안에서 그 회원의 추천 노출 기록을 지운다. */
@Component
@RequiredArgsConstructor
public class MobileRecommendationPurgeListener {

    private final MobileRecommendationRunRepository runs;

    @EventListener
    public void onMemberPurged(MobileMemberPurgedEvent event) {
        runs.deleteByUserId(event.userId());
    }
}
