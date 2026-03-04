package com.raota.domain.ramenShop.controller.contract;

import com.raota.domain.ramenShop.controller.response.VotingStatusResponse;
import com.raota.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "메뉴 투표", description = "메뉴 투표 API")
public interface MenuVoteApi {

    @Operation(summary = "투표 현황 조회", description = "가게별 투표 현황을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<VotingStatusResponse>> getVoteStatus(
            @Parameter(description = "가게 ID", required = true) Long shopId);

    @Operation(summary = "메뉴 투표", description = "특정 메뉴에 투표합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<VotingStatusResponse>> votingMenu(
            @Parameter(description = "가게 ID", required = true) Long shopId,
            @Parameter(description = "메뉴 ID", required = true) Long menuId,
            @Parameter(hidden = true) Long memberId);
}
