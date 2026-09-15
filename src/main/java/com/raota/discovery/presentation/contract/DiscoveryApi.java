package com.raota.discovery.presentation.contract;

import com.raota.discovery.presentation.response.DiscoveryStatsResponse;
import com.raota.global.presentation.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

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

}
