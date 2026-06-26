package com.raota.presentation.api.ramenShop;

import com.raota.application.member.BookmarkService;
import com.raota.application.ramenShop.service.AiRamenShopSearchService;
import com.raota.application.ramenShop.result.AiRamenShopSearchResult;
import com.raota.presentation.api.ramenShop.contract.RamenShopInfoApi;
import com.raota.presentation.api.ramenShop.request.AiRamenShopSearchRequest;
import com.raota.presentation.api.ramenShop.request.RamenShopReportRequest;
import com.raota.presentation.api.ramenShop.response.AiRamenShopSearchResponse;
import com.raota.presentation.api.ramenShop.response.RamenShopResponse;
import com.raota.presentation.api.ramenShop.request.RamenShopSearchRequest;
import com.raota.presentation.api.ramenShop.response.RamenShopBasicInfoResponse;
import com.raota.application.ramenShop.service.RamenShopInfoService;
import com.raota.application.ramenShop.service.RamenShopReportService;
import com.raota.infrastructure.auth.LoginMember;
import com.raota.presentation.common.ApiResponse;
import com.raota.presentation.common.PageResponse;
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
    private final AiRamenShopSearchService aiRamenShopSearchService;
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

    @PostMapping("/ai-search")
    @Override
    public ResponseEntity<ApiResponse<AiRamenShopSearchResponse>> searchAiRamenShops(
            @RequestBody AiRamenShopSearchRequest request,
            @LoginMember(required = false) Long memberId) {
        String query = request == null ? null : request.query();
        return ResponseEntity.ok(ApiResponse.success(toResponse(aiRamenShopSearchService.search(query, memberId))));
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

    private AiRamenShopSearchResponse toResponse(AiRamenShopSearchResult result) {
        return new AiRamenShopSearchResponse(result.shops().stream()
                .map(shop -> new AiRamenShopSearchResponse.RecommendedShopResponse(
                        shop.id(),
                        shop.name(),
                        shop.type(),
                        shop.location(),
                        shop.description(),
                        shop.imageUrl(),
                        shop.matchScore(),
                        shop.bookmarked()
                ))
                .toList());
    }
}
