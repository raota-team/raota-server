package com.raota.agent.presentation.recommendation;
import com.raota.agent.application.recommendation.RecommendationService;
import com.raota.agent.presentation.recommendation.contract.RecommendationApi;
import com.raota.agent.presentation.recommendation.request.*;
import com.raota.agent.presentation.recommendation.response.*;
import com.raota.global.presentation.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RecommendationController implements RecommendationApi {

    private final RecommendationService recommendationService;

    @Override
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> summarizeReviews(ReviewSummaryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recommendationService.summarizeReviews(request)));
    }

    @Override
    public ResponseEntity<ApiResponse<AiChatResponse>> followUpChat(AiChatRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recommendationService.followUpChat(request)));
    }
}
