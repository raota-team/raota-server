package com.raota.agent.presentation.recommendation.contract;

import com.raota.agent.presentation.recommendation.response.TodayRecommendationResponse;
import com.raota.global.presentation.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "오늘의 라멘 추천", description = "홈 화면의 오늘의 라멘 추천 API")
public interface DailyRecommendationApi {

    @Operation(summary = "오늘의 라멘 추천 조회", description = "오늘 사용자에게 추천할 라멘 종류 데이터 1건을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<List<TodayRecommendationResponse>>> getTodayRecommendations();

    @Operation(summary = "오늘의 라멘 추천 수동 생성", description = "날씨 조회, AI 분석, DB/Redis 저장 흐름을 즉시 실행합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<TodayRecommendationResponse>> generateTodayRecommendation();
}
