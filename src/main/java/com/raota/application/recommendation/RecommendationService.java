package com.raota.application.recommendation;
import com.raota.presentation.api.recommendation.request.*;
import com.raota.presentation.api.recommendation.response.*;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {
    public TasteRecommendationResponse recommendByTaste(TasteRecommendationRequest request) { return null; }
    public ShopComparisonResponse compareShops(ShopComparisonRequest request) { return null; }
    public ReviewSummaryResponse summarizeReviews(ReviewSummaryRequest request) { return null; }
    public AiChatResponse followUpChat(AiChatRequest request) { return null; }
}
