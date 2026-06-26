package com.raota.application.recommendation;

import com.raota.domain.recommendation.model.DailyCuration;
import com.raota.infrastructure.file.FileUploader;
import com.raota.presentation.api.discovery.response.TodayRecommendationResponse;
import com.raota.presentation.api.recommendation.request.AiChatRequest;
import com.raota.presentation.api.recommendation.request.ReviewSummaryRequest;
import com.raota.presentation.api.recommendation.response.AiChatResponse;
import com.raota.presentation.api.recommendation.response.ReviewSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ReviewSummaryService reviewSummaryService;
    private final FollowUpChatService followUpChatService;
    private final DailyCurationService dailyCurationService;
    private final FileUploader fileUploader;

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
