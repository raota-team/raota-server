package com.raota.ramenshop.presentation;

import com.raota.ramenshop.application.service.BookmarkService;
import com.raota.ramenshop.presentation.contract.RamenShopInfoApi;
import com.raota.ramenshop.presentation.request.RamenShopReportRequest;
import com.raota.ramenshop.presentation.response.RamenShopMenuOptionsResponse;
import com.raota.ramenshop.presentation.response.RamenShopResponse;
import com.raota.ramenshop.presentation.request.RamenShopSearchRequest;
import com.raota.ramenshop.presentation.response.RamenShopBasicInfoResponse;
import com.raota.ramenshop.application.service.RamenShopInfoService;
import com.raota.ramenshop.application.service.RamenShopReportService;
import com.raota.account.infrastructure.auth.LoginMember;
import com.raota.global.presentation.common.ApiResponse;
import com.raota.global.presentation.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Override
    @GetMapping("/{shopId}/menus")
    public ResponseEntity<ApiResponse<RamenShopMenuOptionsResponse>> getShopMenuOptions(
            @PathVariable Long shopId) {
        return ResponseEntity.ok(ApiResponse.success(ramenShopInfoService.getShopMenuOptions(shopId)));
    }

    @Override
    @PostMapping("/{shopId}/views")
    public ResponseEntity<ApiResponse<Void>> increaseShopViewCount(
            @PathVariable Long shopId) {
        ramenShopInfoService.increaseViewCount(shopId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    @Override
    public ResponseEntity<ApiResponse<PageResponse<RamenShopResponse>>> getShopList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            RamenShopSearchRequest request) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RamenShopResponse> response = ramenShopInfoService.getRamenShopList(request.getCity(),
                request.getDistrict(), request.getKeyword(), request.getTag(), request.getSort(), pageable);
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
