package com.raota.presentation.api.discovery;

import com.raota.application.discovery.DiscoveryService;
import com.raota.application.recommendation.RecommendationService;
import com.raota.domain.recommendation.model.WeekendCuration;
import com.raota.presentation.api.discovery.contract.DiscoveryApi;
import com.raota.presentation.api.discovery.response.DiscoveryStatsResponse;
import com.raota.presentation.api.discovery.response.TrendingTagResponse;
import com.raota.presentation.api.discovery.response.WeekendRecommendationResponse;
import com.raota.presentation.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    @GetMapping("/trending-tags")
    public ResponseEntity<ApiResponse<List<TrendingTagResponse>>> getTrendingTags(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getTrendingTags(limit)));
    }

    @Override
    @GetMapping("/weekend-recommendations")
    public ResponseEntity<ApiResponse<List<WeekendRecommendationResponse>>> getWeekendRecommendations() {
        WeekendRecommendationResponse recommendation = recommendationService.getWeekendRecommendation();
        
        if (recommendation == null) {
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }
        
        return ResponseEntity.ok(ApiResponse.success(List.of(recommendation)));
    }
}
