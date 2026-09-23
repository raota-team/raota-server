package com.raota.agent.presentation.recommendation;

import com.raota.agent.application.recommendation.RecommendationService;
import com.raota.agent.presentation.recommendation.contract.DailyRecommendationApi;
import com.raota.agent.presentation.recommendation.response.TodayRecommendationResponse;
import com.raota.global.presentation.common.ApiResponse;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/discovery")
@RequiredArgsConstructor
public class DailyRecommendationController implements DailyRecommendationApi {

    private final RecommendationService recommendationService;

    @Override
    @GetMapping("/today-recommendations")
    public ResponseEntity<ApiResponse<List<TodayRecommendationResponse>>> getTodayRecommendations() {
        TodayRecommendationResponse recommendation = recommendationService.getTodayRecommendation();

        if (recommendation == null) {
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }

        return ResponseEntity.ok(ApiResponse.success(List.of(recommendation)));
    }

    @Override
    @PostMapping("/today-recommendations/generate")
    public ResponseEntity<ApiResponse<TodayRecommendationResponse>> generateTodayRecommendation() {
        return ResponseEntity.ok(ApiResponse.success(recommendationService.generateTodayRecommendation()));
    }

}
