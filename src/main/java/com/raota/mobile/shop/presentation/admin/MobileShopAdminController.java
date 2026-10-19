package com.raota.mobile.shop.presentation.admin;

import com.raota.mobile.common.page.OffsetPage;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.shop.application.result.MobileAdminShopDetail;
import com.raota.mobile.shop.application.result.MobileAdminShopRow;
import com.raota.mobile.shop.application.service.MobileShopAdminService;
import com.raota.mobile.shop.presentation.admin.request.MobileAdminBusinessHoursRequest;
import com.raota.mobile.shop.presentation.admin.request.MobileAdminServicePerksRequest;
import com.raota.mobile.shop.presentation.admin.request.MobileAdminShopRequest;
import com.raota.mobile.shop.presentation.admin.request.MobileAdminVisibilityRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 웹(raota-front)의 앱 매장 관리 API다. 웹 관리자 인증({@code /admin/**} ADMIN)을 쓰고 응답은 v2 형식이다.
 */
@RestController
@RequestMapping("/admin/api/v2/shops")
@RequiredArgsConstructor
public class MobileShopAdminController {

    private final MobileShopAdminService shops;

    @GetMapping
    public MobileApiResponse<OffsetPage<MobileAdminShopRow>> list(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean published, @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(OffsetPage.MAX_SIZE) int size) {
        return MobileApiResponse.success(shops.list(keyword, published, page, size));
    }

    @GetMapping("/{shopId:[0-9]+}")
    public MobileApiResponse<MobileAdminShopDetail> detail(@PathVariable Long shopId) {
        return MobileApiResponse.success(shops.detail(shopId));
    }

    @PostMapping
    public ResponseEntity<MobileApiResponse<MobileAdminShopDetail>> create(
            @RequestBody @Valid MobileAdminShopRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(MobileApiResponse.success(shops.create(request.toCommand())));
    }

    @PutMapping("/{shopId:[0-9]+}")
    public MobileApiResponse<MobileAdminShopDetail> update(@PathVariable Long shopId,
            @RequestBody @Valid MobileAdminShopRequest request) {
        return MobileApiResponse.success(shops.update(shopId, request.toCommand()));
    }

    @PutMapping("/{shopId:[0-9]+}/business-hours")
    public MobileApiResponse<MobileAdminShopDetail> replaceBusinessHours(@PathVariable Long shopId,
            @RequestBody @Valid MobileAdminBusinessHoursRequest request) {
        return MobileApiResponse.success(shops.replaceBusinessHours(shopId, request.toCommands()));
    }

    @PutMapping("/{shopId:[0-9]+}/service-perks")
    public MobileApiResponse<MobileAdminShopDetail> replaceServicePerks(@PathVariable Long shopId,
            @RequestBody @Valid MobileAdminServicePerksRequest request) {
        return MobileApiResponse.success(shops.replaceServicePerks(shopId, request.toCommands()));
    }

    @PatchMapping("/{shopId:[0-9]+}/visibility")
    public MobileApiResponse<MobileAdminShopDetail> changeVisibility(@PathVariable Long shopId,
            @RequestBody @Valid MobileAdminVisibilityRequest request) {
        return MobileApiResponse.success(shops.changeVisibility(shopId, request.published()));
    }

}
