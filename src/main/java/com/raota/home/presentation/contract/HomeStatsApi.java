package com.raota.home.presentation.contract;

import com.raota.global.presentation.common.ApiResponse;
import com.raota.home.presentation.response.HomeStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "홈 화면 - 통계", description = "홈 화면의 공개 통계 API")
public interface HomeStatsApi {

    @Operation(summary = "홈 통계 조회", description = "등록된 라멘집, 누적 리뷰 또는 AI 분석, 사용자 통계를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<HomeStatsResponse>> getHomeStats();
}
