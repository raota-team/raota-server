package com.raota.application.recommendation;

import com.raota.application.ramenShop.service.AiRamenShopSearchService;
import com.raota.domain.recommendation.model.DailyCuration;
import com.raota.infrastructure.file.FileUploader;
import com.raota.presentation.api.discovery.response.TodayRecommendationResponse;
import com.raota.presentation.api.recommendation.request.*;
import com.raota.presentation.api.recommendation.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final AiRamenShopSearchService aiRamenShopSearchService;
    private final ShopComparisonService shopComparisonService;
    private final ReviewSummaryService reviewSummaryService;
    private final FollowUpChatService followUpChatService;
    private final DailyCurationService dailyCurationService;
    private final FileUploader fileUploader;

    public TasteRecommendationResponse recommendByTaste(TasteRecommendationRequest request, Long memberId) {
        return aiRamenShopSearchService.recommendByTaste(request, memberId);
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

    public TodayRecommendationResponse getTodayRecommendation() {
        return dailyCurationService.getLatestCuration()
                .map(this::toTodayRecommendationResponse)
                .orElse(null);
    }

    public TodayRecommendationResponse generateTodayRecommendation() {
        return toTodayRecommendationResponse(dailyCurationService.generateDailyCuration());
    }

    private TodayRecommendationResponse toTodayRecommendationResponse(DailyCuration curation) {
        String imageUrl = fileUploader.getAccessibleUrl(curation.getEffectiveImageUrl());
        return TodayRecommendationResponse.from(curation, imageUrl);
    }
}
