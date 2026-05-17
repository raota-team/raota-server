package com.raota.presentation.api.ramenShop.contract;

import com.raota.presentation.api.ramenShop.dto.RamenShopReportRequest;
import com.raota.presentation.api.ramenShop.dto.RamenShopSearchRequest;
import com.raota.presentation.api.ramenShop.dto.RamenShopBasicInfoResponse;
import com.raota.presentation.api.ramenShop.dto.StoreSummaryResponse;
import com.raota.presentation.common.ApiResponse;
import com.raota.presentation.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "라멘 가게", description = "라멘 가게 조회/방문 API")
public interface RamenShopInfoApi {

    @Operation(summary = "가게 상세 조회", description = "가게의 기본 정보와 메뉴, 통계를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<RamenShopBasicInfoResponse>> getShopDetailInfo(
            @Parameter(description = "가게 ID", required = true) Long shopId,
            Long memberId);

    @Operation(summary = "가게 목록 조회", description = "지역/키워드 조건과 페이징으로 가게 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    @GetMapping
    ResponseEntity<ApiResponse<PageResponse<StoreSummaryResponse>>> getShopList(
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "12")
            @RequestParam(defaultValue = "12") int size,
            RamenShopSearchRequest request);

    @Operation(summary = "가게 북마크 토글", description = "가게를 찜하거나 해제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<Boolean>> toggleBookmark(
            @Parameter(description = "가게 ID", required = true) Long shopId,
            @Parameter(hidden = true) Long memberId);

    @Operation(summary = "가게 정보 제보하기", description = "가게의 영업시간 오류, 폐업 등을 제보합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<Void>> reportShop(
            @Parameter(description = "가게 ID", required = true) Long shopId,
            @Parameter(hidden = true) Long memberId,
            RamenShopReportRequest request);
}
