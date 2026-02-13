package com.raota.domain.ramenShop.controller.contract;

import com.raota.domain.ramenShop.controller.request.RamenShopSearchRequest;
import com.raota.domain.ramenShop.controller.request.VisitCertificationRequest;
import com.raota.domain.ramenShop.controller.response.RamenShopBasicInfoResponse;
import com.raota.domain.ramenShop.controller.response.StoreSummaryResponse;
import com.raota.domain.ramenShop.controller.response.VisitCountingResponse;
import com.raota.domain.ramenShop.controller.response.WaitingSpotResponse;
import com.raota.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "RamenShop", description = "라멘 가게 조회/방문 API")
public interface RamenShopInfoApi {

    @Operation(summary = "방문 인증", description = "가게 방문 인증 후 방문 수를 증가시킵니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<VisitCountingResponse>> addVisitCount(
            @Parameter(description = "가게 ID", required = true) Long shopId,
            VisitCertificationRequest request);

    @Operation(summary = "주변 대기 장소 조회", description = "가게 주변의 대기 가능한 장소 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<WaitingSpotResponse>> getNearByPlace(
            @Parameter(description = "가게 ID", required = true) Long shopId);

    @Operation(summary = "가게 상세 조회", description = "가게의 기본 정보와 메뉴, 통계를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<RamenShopBasicInfoResponse>> getShopDetailInfo(
            @Parameter(description = "가게 ID", required = true) Long shopId);

    @Operation(summary = "가게 목록 조회", description = "지역/키워드 조건과 페이징으로 가게 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<Page<StoreSummaryResponse>>> getShopDetailInfo(
            @ParameterObject Pageable pageable,
            @ParameterObject RamenShopSearchRequest request);
}
