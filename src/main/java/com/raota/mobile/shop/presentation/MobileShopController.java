package com.raota.mobile.shop.presentation;

import com.raota.mobile.common.cursor.CursorPage;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.shop.application.query.MobileShopSort;
import com.raota.mobile.shop.application.result.MobileShopMapPin;
import com.raota.mobile.shop.application.result.MobileShopSummary;
import com.raota.mobile.shop.application.service.MobileShopQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 매장 목록과 지도 탭의 공개 조회 API다. */
@RestController
@RequestMapping("/api/v2/shops")
@RequiredArgsConstructor
public class MobileShopController {

    private final MobileShopQueryService shops;

    @GetMapping
    public MobileApiResponse<CursorPage<MobileShopSummary>> list(
            @RequestParam(defaultValue = "POPULAR") MobileShopSort sort, @RequestParam(required = false) String query,
            @RequestParam(required = false) String region, @RequestParam(required = false) String ramenType,
            @RequestParam(defaultValue = "false") boolean openNow, @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude, @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(CursorPage.MAX_SIZE) int size) {
        return MobileApiResponse
            .success(shops.list(sort, query, region, ramenType, openNow, latitude, longitude, cursor, size));
    }

    @GetMapping("/map-pins")
    public MobileApiResponse<List<MobileShopMapPin>> mapPins() {
        return MobileApiResponse.success(shops.mapPins());
    }

}
