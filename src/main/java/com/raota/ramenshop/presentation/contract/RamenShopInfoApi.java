package com.raota.ramenshop.presentation.contract;

import com.raota.ramenshop.presentation.request.RamenShopReportRequest;
import com.raota.ramenshop.presentation.response.RamenShopMenuOptionsResponse;
import com.raota.ramenshop.presentation.response.RamenShopResponse;
import com.raota.ramenshop.presentation.request.RamenShopSearchRequest;
import com.raota.ramenshop.presentation.response.RamenShopBasicInfoResponse;
import com.raota.global.presentation.common.ApiResponse;
import com.raota.global.presentation.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.raota.ramenshop.presentation.response.RecentVerifiedShopResponse;
import java.util.List;
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

    @Operation(summary = "가게 메뉴 옵션 조회", description = "라멘로그 작성 시 선택할 수 있는 가게의 일반 메뉴와 이벤트 메뉴 이름을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<RamenShopMenuOptionsResponse>> getShopMenuOptions(
            @Parameter(description = "가게 ID", required = true) Long shopId);

    @Operation(summary = "가게 조회수 증가", description = "가게의 조회수를 1 증가시킵니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<Void>> increaseShopViewCount(
            @Parameter(description = "가게 ID", required = true) Long shopId);

    @Operation(summary = "가게 목록 조회", description = "지역/키워드 조건과 페이징으로 가게 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    @GetMapping
    ResponseEntity<ApiResponse<PageResponse<RamenShopResponse>>> getShopList(
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
