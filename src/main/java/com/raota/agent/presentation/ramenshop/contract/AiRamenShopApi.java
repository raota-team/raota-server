package com.raota.agent.presentation.ramenshop.contract;

import com.raota.agent.presentation.ramenshop.request.AiRamenShopSearchRequest;
import com.raota.agent.presentation.ramenshop.request.RamenShopComparisonRequest;
import com.raota.agent.presentation.ramenshop.response.AiRamenShopSearchResponse;
import com.raota.agent.presentation.ramenshop.response.RamenShopComparisonResponse;
import com.raota.global.presentation.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "라멘 가게", description = "AI 라멘 가게 검색/비교 API")
public interface AiRamenShopApi {

    @Operation(summary = "AI 가게 검색")
    ResponseEntity<ApiResponse<AiRamenShopSearchResponse>> search(
            AiRamenShopSearchRequest request,
            @Parameter(hidden = true) Long memberId);

    @Operation(summary = "라멘 가게 1:1 비교")
    ResponseEntity<ApiResponse<RamenShopComparisonResponse>> compare(RamenShopComparisonRequest request);
}
