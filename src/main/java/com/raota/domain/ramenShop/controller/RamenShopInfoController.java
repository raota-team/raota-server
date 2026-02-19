package com.raota.domain.ramenShop.controller;

import com.raota.domain.ramenShop.controller.contract.RamenShopInfoApi;
import com.raota.domain.ramenShop.controller.request.RamenShopSearchRequest;
import com.raota.global.common.ApiResponse;
import com.raota.domain.ramenShop.controller.response.RamenShopBasicInfoResponse;
import com.raota.domain.ramenShop.controller.response.StoreSummaryResponse;
import com.raota.domain.ramenShop.service.RamenShopInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ramen-shops")
@RequiredArgsConstructor
public class RamenShopInfoController implements RamenShopInfoApi {

    private final RamenShopInfoService ramenShopInfoService;

    @Override
    @GetMapping("/{shopId}")
    public ResponseEntity<ApiResponse<RamenShopBasicInfoResponse>> getShopDetailInfo(@PathVariable Long shopId) {
        RamenShopBasicInfoResponse response = ramenShopInfoService.getShopDetailInfo(shopId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<Page<StoreSummaryResponse>>> getShopDetailInfo(
            @PageableDefault(size = 12, direction = Sort.Direction.DESC) Pageable pageable,
            RamenShopSearchRequest request) {
        Page<StoreSummaryResponse> response = ramenShopInfoService.getRamenShopList(request.getRegion(),request.getKeyword(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
