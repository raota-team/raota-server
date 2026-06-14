package com.raota.application.recommendation;

import com.raota.domain.recommendation.model.WeekendCuration;
import com.raota.infrastructure.file.FileUploader;
import com.raota.presentation.api.discovery.response.WeekendRecommendationResponse;
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
    private final WeekendCurationService weekendCurationService;
    private final FileUploader fileUploader;

    public TasteRecommendationResponse recommendByTaste(TasteRecommendationRequest request, Long memberId) {
        return tasteRecommendationService.recommendByTaste(request, memberId);
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

    public com.raota.presentation.api.discovery.response.WeekendRecommendationResponse getWeekendRecommendation() {
        return weekendCurationService.getLatestCuration()
                .map(this::toWeekendRecommendationResponse)
                .orElse(null);
    }

    public WeekendRecommendationResponse generateWeekendRecommendation() {
        return toWeekendRecommendationResponse(weekendCurationService.generateWeeklyCuration());
    }

    private WeekendRecommendationResponse toWeekendRecommendationResponse(WeekendCuration curation) {
        String imageUrl = fileUploader.getAccessibleUrl(curation.getEffectiveImageUrl());
        return WeekendRecommendationResponse.from(curation, imageUrl);
    }
}
