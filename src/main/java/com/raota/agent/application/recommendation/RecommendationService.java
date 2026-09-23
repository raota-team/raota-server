package com.raota.agent.application.recommendation;

import com.raota.agent.domain.recommendation.model.DailyCuration;
import com.raota.agent.application.recommendation.query.FollowUpChatQuery;
import com.raota.agent.application.recommendation.query.ReviewSummaryQuery;
import com.raota.global.file.FileUploader;
import com.raota.agent.presentation.recommendation.response.AiChatResponse;
import com.raota.agent.presentation.recommendation.response.ReviewSummaryResponse;
import com.raota.agent.presentation.recommendation.response.TodayRecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ReviewSummaryService reviewSummaryService;

    private final FollowUpChatService followUpChatService;

    private final DailyCurationService dailyCurationService;

    private final FileUploader fileUploader;

    public ReviewSummaryResponse summarizeReviews(ReviewSummaryQuery query) {
        return reviewSummaryService.summarizeReviews(query);
    }

    public AiChatResponse followUpChat(FollowUpChatQuery query) {
        return followUpChatService.followUpChat(query);
    }

    public TodayRecommendationResponse getTodayRecommendation() {
        return dailyCurationService.getLatestCuration().map(this::toTodayRecommendationResponse).orElse(null);
    }

    public TodayRecommendationResponse generateTodayRecommendation() {
        return toTodayRecommendationResponse(dailyCurationService.generateDailyCuration());
    }

    private TodayRecommendationResponse toTodayRecommendationResponse(DailyCuration curation) {
        String imageUrl = fileUploader.getAccessibleUrl(curation.getEffectiveImageUrl());
        return TodayRecommendationResponse.from(curation, imageUrl);
    }

}
