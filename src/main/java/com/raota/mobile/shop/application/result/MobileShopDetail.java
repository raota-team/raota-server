package com.raota.mobile.shop.application.result;

import com.raota.mobile.shop.domain.model.MobileShopBusinessStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** 한 매장 소개의 모든 확인된 정보와 사진·혜택·요일별 영업시간이다. */
public record MobileShopDetail(String id, String name, String branchName, String address, String region,
        BigDecimal latitude, BigDecimal longitude, String imageUrl, String tagline, List<String> ramenTypes,
        List<String> tags, int logCount, int bookmarkCount, boolean isBookmarked,
        MobileShopBusinessStatus businessStatus, Boolean isOpen, Integer distanceMeters, String description,
        String phone, String instagramUrl, String reservationUrl, String websiteUrl, String naverPlaceId,
        String kakaoPlaceId, Integer priceMin, Integer priceMax, String closedDaysText, Instant hoursVerifiedAt,
        List<MobileShopImageResult> images, List<MobileShopBusinessHourResult> businessHours,
        List<MobileShopServicePerkResult> servicePerks, String aiReviewSummary, List<String> aiSummaryKeywords,
        Instant aiSummaryGeneratedAt) {
}
