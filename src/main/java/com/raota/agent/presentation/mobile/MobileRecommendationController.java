package com.raota.agent.presentation.mobile;

import com.raota.agent.application.mobile.MobileRecommendationService;
import com.raota.agent.application.mobile.result.MobileRecommendationResult;
import com.raota.agent.application.mobile.result.MobileTodayRecommendationsResult;
import com.raota.agent.presentation.mobile.request.MobileRecommendationRequest;
import com.raota.mobile.common.presentation.LoginUser;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 앱 AI 큐레이터의 추천 API다. LLM·검색 비용 때문에 로그인한 회원만 쓸 수 있고, 하루에 뽑을 수 있는 식권 수가 정해져 있다. */
@RestController
@RequestMapping("/api/v2/ai")
@RequiredArgsConstructor
public class MobileRecommendationController {

    private final MobileRecommendationService recommendations;

    @PostMapping("/recommendations")
    public MobileApiResponse<MobileRecommendationResult> recommend(@LoginUser Long userId,
            @Valid @RequestBody MobileRecommendationRequest request) {
        return MobileApiResponse.success(recommendations.recommend(userId, request.toCommand()));
    }

    /** 오늘 뽑은 식권(최신순)과 남은 장수다. */
    @GetMapping("/recommendations/today")
    public MobileApiResponse<MobileTodayRecommendationsResult> today(@LoginUser Long userId) {
        return MobileApiResponse.success(recommendations.today(userId));
    }

}
