package com.raota.mobile.shop.domain.model;

import java.math.BigDecimal;
import java.util.List;

/** 관리자가 편집하는 매장 기본 정보다. 집계·AI 요약·공개 여부는 따로 바뀐다. */
public record MobileShopProfile(String name, String branchName, String address, String region, BigDecimal latitude,
        BigDecimal longitude, String phone, String tagline, String description, List<String> ramenTypes,
        List<String> tags, String instagramUrl, String reservationUrl, String websiteUrl, String naverPlaceId,
        String kakaoPlaceId, Integer priceMin, Integer priceMax, MobileShopBusinessStatus businessStatus,
        String closedDaysText) {

    public MobileShopProfile {
        ramenTypes = List.copyOf(ramenTypes);
        tags = List.copyOf(tags);
    }

}
