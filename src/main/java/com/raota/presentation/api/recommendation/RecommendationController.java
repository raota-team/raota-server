package com.raota.presentation.api.recommendation;
import com.raota.application.recommendation.RecommendationService;
import com.raota.presentation.api.recommendation.contract.RecommendationApi;
import com.raota.presentation.api.recommendation.request.*;
import com.raota.presentation.api.recommendation.response.*;
import com.raota.presentation.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RecommendationController implements RecommendationApi {

    private final RecommendationService recommendationService;

    @Override
    public ResponseEntity<ApiResponse<ShopComparisonResponse>> compareShops(ShopComparisonRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recommendationService.compareShops(request)));
    }

    @Override
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> summarizeReviews(ReviewSummaryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recommendationService.summarizeReviews(request)));
    }

    @Override
    public ResponseEntity<ApiResponse<AiChatResponse>> followUpChat(AiChatRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recommendationService.followUpChat(request)));
    }
}
