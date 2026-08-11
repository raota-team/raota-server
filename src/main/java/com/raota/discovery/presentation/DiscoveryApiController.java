package com.raota.discovery.presentation;

import com.raota.discovery.application.DiscoveryService;
import com.raota.agent.application.recommendation.RecommendationService;
import com.raota.discovery.presentation.contract.DiscoveryApi;
import com.raota.discovery.presentation.response.DiscoveryStatsResponse;
import com.raota.ramenshop.application.result.TodayPopularRamenShopResponse;
import com.raota.agent.presentation.recommendation.response.TodayRecommendationResponse;
import com.raota.global.presentation.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/discovery")
@RequiredArgsConstructor
public class DiscoveryApiController implements DiscoveryApi {

    private final DiscoveryService discoveryService;
    private final RecommendationService recommendationService;

    @Override
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DiscoveryStatsResponse>> getDiscoveryStats() {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getStats()));
    }

    @Override
    @GetMapping("/popular-shops/today")
    public ResponseEntity<ApiResponse<List<TodayPopularRamenShopResponse>>> getTodayPopularShops(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getTodayPopularShops(limit)));
    }

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
