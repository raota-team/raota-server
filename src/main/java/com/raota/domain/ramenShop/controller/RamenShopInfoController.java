package com.raota.domain.ramenShop.controller;

import com.raota.domain.member.service.BookmarkService;
import com.raota.domain.ramenShop.controller.contract.RamenShopInfoApi;
import com.raota.domain.ramenShop.controller.request.RamenShopReportRequest;
import com.raota.domain.ramenShop.controller.request.RamenShopSearchRequest;
import com.raota.domain.ramenShop.controller.response.RamenShopBasicInfoResponse;
import com.raota.domain.ramenShop.controller.response.StoreSummaryResponse;
import com.raota.domain.ramenShop.service.RamenShopInfoService;
import com.raota.domain.ramenShop.service.RamenShopReportService;
import com.raota.global.auth.LoginMember;
import com.raota.global.common.ApiResponse;
import com.raota.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ramen-shops")
@RequiredArgsConstructor
public class RamenShopInfoController implements RamenShopInfoApi {

    private final RamenShopInfoService ramenShopInfoService;
    private final BookmarkService bookmarkService;
    private final RamenShopReportService reportService;

    @Override
    @GetMapping("/{shopId}")
    public ResponseEntity<ApiResponse<RamenShopBasicInfoResponse>> getShopDetailInfo(
            @PathVariable Long shopId,
            @LoginMember(required = false) Long memberId) {
        RamenShopBasicInfoResponse response = ramenShopInfoService.getShopDetailInfo(shopId,memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Override
    public ResponseEntity<ApiResponse<PageResponse<StoreSummaryResponse>>> getShopList(
            @PageableDefault(size = 12, direction = Sort.Direction.DESC) Pageable pageable,
            RamenShopSearchRequest request) {
        Page<StoreSummaryResponse> response = ramenShopInfoService.getRamenShopList(request.getRegion(), request.getKeyword(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }

    @Override
    @PostMapping("/{shopId}/bookmark")
    public ResponseEntity<ApiResponse<Boolean>> toggleBookmark(
            @PathVariable Long shopId,
            @LoginMember Long memberId) {
        boolean result = bookmarkService.toggleBookmark(memberId, shopId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Override
    @PostMapping("/{shopId}/reports")
    public ResponseEntity<ApiResponse<Void>> reportShop(
            @PathVariable Long shopId,
            @LoginMember Long memberId,
            @RequestBody RamenShopReportRequest request) {
        reportService.reportShop(shopId, memberId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
