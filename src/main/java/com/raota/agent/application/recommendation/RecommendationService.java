package com.raota.agent.application.recommendation;

import com.raota.agent.domain.recommendation.model.DailyCuration;
import com.raota.agent.application.recommendation.query.FollowUpChatQuery;
import com.raota.agent.application.recommendation.query.ReviewSummaryQuery;
import com.raota.global.file.FileUploader;
import com.raota.agent.presentation.recommendation.request.AiChatRequest;
import com.raota.agent.presentation.recommendation.request.ReviewSummaryRequest;
import com.raota.agent.presentation.recommendation.response.TodayRecommendationResponse;
import com.raota.agent.presentation.recommendation.response.AiChatResponse;
import com.raota.agent.presentation.recommendation.response.ReviewSummaryResponse;
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
        if (request == null) {
            return reviewSummaryService.summarizeReviews(null);
        }
        return reviewSummaryService.summarizeReviews(new ReviewSummaryQuery(request.shopId(), request.focus()));
    }

    public AiChatResponse followUpChat(AiChatRequest request) {
        if (request == null) {
            return followUpChatService.followUpChat(null);
        }
        var messages = request.messages() == null
                ? null
                : request.messages().stream()
                        .map(message -> message == null
                                ? null
                                : new FollowUpChatQuery.Message(message.role(), message.content()))
                        .toList();
        return followUpChatService.followUpChat(new FollowUpChatQuery(
                request.contextType(),
                request.shopIds(),
                messages
        ));
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
