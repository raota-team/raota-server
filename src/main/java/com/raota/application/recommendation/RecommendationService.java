package com.raota.application.recommendation;

import com.raota.presentation.api.recommendation.request.*;
import com.raota.presentation.api.recommendation.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final TasteRecommendationService tasteRecommendationService;
    private final ShopComparisonService shopComparisonService;
    private final ReviewSummaryService reviewSummaryService;
    private final FollowUpChatService followUpChatService;

    // Recommendation API의 진입점으로, 세부 AI 유스케이스를 각 전용 서비스에 위임한다.
    public TasteRecommendationResponse recommendByTaste(TasteRecommendationRequest request) {
        return tasteRecommendationService.recommendByTaste(request);
    }

    public ShopComparisonResponse compareShops(ShopComparisonRequest request) {
        return shopComparisonService.compareShops(request);
    }


    public ReviewSummaryResponse summarizeReviews(ReviewSummaryRequest request) {
        return reviewSummaryService.summarizeReviews(request);
    }

    public AiChatResponse followUpChat(AiChatRequest request) {
        return followUpChatService.followUpChat(request);
    }
}
