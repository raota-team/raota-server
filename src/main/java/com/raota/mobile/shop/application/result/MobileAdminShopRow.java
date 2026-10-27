package com.raota.mobile.shop.application.result;

import com.raota.mobile.shop.domain.model.MobileShopBusinessStatus;
import java.time.Instant;

/** 관리자 매장 목록의 한 행이다. 숨김 매장도 포함한다. */
public record MobileAdminShopRow(
        String id,
        String name,
        String branchName,
        String address,
        String region,
        MobileShopBusinessStatus businessStatus,
        boolean published,
        String thumbnailUrl,
        int logCount,
        int bookmarkCount,
        Instant hoursVerifiedAt,
        Instant updatedAt) {}
