package com.raota.mobile.shop.presentation;

import com.raota.mobile.common.cursor.CursorPage;
import com.raota.mobile.common.presentation.LoginUser;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.shop.application.result.MobileShopDetailResult;
import com.raota.mobile.shop.application.result.MobileShopSummaryResult;
import com.raota.mobile.shop.application.service.MobileShopQueryService;
import com.raota.mobile.shop.presentation.request.MobileShopSearchRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 매장 목록과 지도 탭의 공개 조회 API다. */
@RestController
@RequestMapping("/api/v2/shops")
@RequiredArgsConstructor
public class MobileShopController {

    private final MobileShopQueryService queryService;

    @GetMapping
    public MobileApiResponse<CursorPage<MobileShopSummaryResult>> list(
            @LoginUser(required = false) Long userId,
            @ModelAttribute MobileShopSearchRequest search,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(CursorPage.MAX_SIZE) int size) {
        return MobileApiResponse.success(queryService.searchPage(userId, search.toCondition(), cursor, size));
    }

    /** 지도 탭의 핀과 목록. 조건은 목록과 같고 페이지 없이 맞는 매장을 모두 준다. */
    @GetMapping("/map")
    public MobileApiResponse<List<MobileShopSummaryResult>> map(
            @LoginUser(required = false) Long userId, @ModelAttribute MobileShopSearchRequest search) {
        return MobileApiResponse.success(queryService.searchAll(userId, search.toCondition()));
    }

    @GetMapping("/{shopId}")
    public MobileApiResponse<MobileShopDetailResult> detail(
            @LoginUser(required = false) Long userId, @PathVariable Long shopId) {
        return MobileApiResponse.success(queryService.viewDetail(userId, shopId));
    }
}
