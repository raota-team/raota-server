package com.raota.presentation.api.recommendation.contract;
import com.raota.presentation.api.recommendation.request.*;
import com.raota.presentation.api.recommendation.response.*;
import com.raota.presentation.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "AI Recommendation API", description = "AI를 이용한 추천, 요약, 채팅 기능을 제공합니다.")
@RequestMapping("/recommendations")
public interface RecommendationApi {

    @Operation(summary = "매장 리뷰 요약")
    @PostMapping("/summary")
    ResponseEntity<ApiResponse<ReviewSummaryResponse>> summarizeReviews(@RequestBody ReviewSummaryRequest request);

    @Operation(summary = "AI 추가 채팅")
    @PostMapping("/chat")
    ResponseEntity<ApiResponse<AiChatResponse>> followUpChat(@RequestBody AiChatRequest request);
}
