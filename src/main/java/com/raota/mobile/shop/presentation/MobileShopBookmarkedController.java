package com.raota.mobile.shop.presentation;

import com.raota.mobile.common.cursor.CursorPage;
import com.raota.mobile.common.presentation.LoginUser;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.shop.application.result.MobileShopSummary;
import com.raota.mobile.shop.application.service.MobileShopQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 로그인한 회원의 가고 싶어요 목록을 조회한다. */
@RestController
@RequestMapping("/api/v2/members/me/bookmarked-shops")
@RequiredArgsConstructor
public class MobileShopBookmarkedController {

    private final MobileShopQueryService shops;

    @GetMapping
    public MobileApiResponse<CursorPage<MobileShopSummary>> bookmarked(@LoginUser Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(CursorPage.MAX_SIZE) int size) {
        return MobileApiResponse.success(shops.bookmarked(userId, cursor, size));
    }

}
