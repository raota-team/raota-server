package com.raota.presentation.api.discovery.contract;

import com.raota.presentation.api.discovery.response.DiscoveryStatsResponse;
import com.raota.presentation.api.discovery.response.TodayPopularRamenShopResponse;
import com.raota.presentation.api.discovery.response.WeekendRecommendationResponse;
import com.raota.presentation.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "탐색", description = "탐색(Discovery) 관련 API")
public interface DiscoveryApi {

    /**
     * 탐색 통계 조회
     */
    @Operation(summary = "탐색 통계 조회",
            description = "등록된 라멘집, 누적 리뷰(또는 AI 분석), 활동 중인 유저 통계를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<DiscoveryStatsResponse>> getDiscoveryStats();

    @Operation(summary = "오늘 많이 본 라멘집 조회", description = "오늘 00시부터 현재까지 상세 조회가 많은 라멘집을 순위순으로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<List<TodayPopularRamenShopResponse>>> getTodayPopularShops(
            @Parameter(description = "가져올 라멘집 개수", example = "5")
            int limit);

    @Operation(summary = "이번 주말의 라멘 추천 조회", description = "이번 주말에 사용자에게 추천할 라멘 종류 데이터 1건을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<List<WeekendRecommendationResponse>>> getWeekendRecommendations();

    @Operation(summary = "이번 주말의 라멘 추천 수동 생성", description = "날씨 조회, AI 분석, DB/Redis 저장 흐름을 즉시 실행합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<WeekendRecommendationResponse>> generateWeekendRecommendation();
}
