package com.raota.presentation.api.discovery.contract;

import com.raota.presentation.api.discovery.response.DiscoveryStatsResponse;
import com.raota.presentation.api.discovery.response.TrendingTagResponse;
import com.raota.presentation.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "탐색", description = "탐색(Discovery) 관련 API")
public interface DiscoveryApi {

    @Operation(summary = "탐색 통계 조회",
            description = "등록된 라멘집, 누적 리뷰(또는 AI 분석), 활동 중인 유저 통계를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<DiscoveryStatsResponse>> getDiscoveryStats();

    @Operation(summary = "실시간 인기 검색어 조회", description = "최근 가장 많이 검색되거나 클릭된 검색어(태그) 순위를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<List<TrendingTagResponse>>> getTrendingTags(
            @Parameter(description = "가져올 순위 개수", example = "5")
            int limit);
}
