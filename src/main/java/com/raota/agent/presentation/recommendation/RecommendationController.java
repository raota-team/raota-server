package com.raota.agent.presentation.recommendation;

import com.raota.agent.application.recommendation.RecommendationService;
import com.raota.agent.application.recommendation.query.FollowUpChatQuery;
import com.raota.agent.application.recommendation.query.ReviewSummaryQuery;
import com.raota.agent.presentation.recommendation.contract.RecommendationApi;
import com.raota.agent.presentation.recommendation.request.AiChatRequest;
import com.raota.agent.presentation.recommendation.request.ReviewSummaryRequest;
import com.raota.agent.presentation.recommendation.response.AiChatResponse;
import com.raota.agent.presentation.recommendation.response.ReviewSummaryResponse;
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
        ReviewSummaryQuery query = request == null ? null : new ReviewSummaryQuery(request.shopId(), request.focus());
        return ResponseEntity.ok(ApiResponse.success(recommendationService.summarizeReviews(query)));
    }

    @Override
    public ResponseEntity<ApiResponse<AiChatResponse>> followUpChat(AiChatRequest request) {
        if (request == null) {
            return ResponseEntity.ok(ApiResponse.success(recommendationService.followUpChat(null)));
        }
        var messages = request.messages() == null
                ? null
                : request.messages().stream()
                        .map(message -> message == null
                                ? null
                                : new FollowUpChatQuery.Message(message.role(), message.content()))
                        .toList();
        FollowUpChatQuery query = new FollowUpChatQuery(request.contextType(), request.shopIds(), messages);
        return ResponseEntity.ok(ApiResponse.success(recommendationService.followUpChat(query)));
    }
}
