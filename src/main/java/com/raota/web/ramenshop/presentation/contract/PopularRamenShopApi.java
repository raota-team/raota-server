package com.raota.web.ramenshop.presentation.contract;

import com.raota.global.presentation.common.ApiResponse;
import com.raota.web.ramenshop.application.result.TodayPopularRamenShopResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "홈 화면 - 인기 라멘 가게", description = "홈 화면의 인기 라멘 가게 API")
public interface PopularRamenShopApi {

    @Operation(summary = "오늘 많이 본 라멘집 조회", description = "오늘 00시부터 현재까지 상세 조회가 많은 라멘집을 순위순으로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<List<TodayPopularRamenShopResponse>>> getTodayPopularShops(
            @Parameter(description = "가져올 라멘집 개수", example = "5")
            int limit);
}
