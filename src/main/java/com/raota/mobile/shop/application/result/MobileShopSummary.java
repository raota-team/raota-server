package com.raota.mobile.shop.application.result;

import com.raota.mobile.shop.domain.model.MobileShopBusinessStatus;
import java.math.BigDecimal;
import java.util.List;

/** 앱 목록의 한 매장과 현재 회원의 가고 싶어요 상태다. */
public record MobileShopSummary(String id, String name, String branchName, String address, String region,
        BigDecimal latitude, BigDecimal longitude, String imageUrl, String tagline, List<String> ramenTypes,
        List<String> tags, int logCount, int bookmarkCount, boolean isBookmarked,
        MobileShopBusinessStatus businessStatus, Boolean isOpen, Integer distanceMeters) {
}
