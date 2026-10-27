package com.raota.mobile.shop.application.result;

import com.raota.mobile.shop.domain.model.MobileShopBusinessStatus;
import com.raota.mobile.shop.domain.model.ServicePerkStatus;
import com.raota.mobile.shop.domain.model.ServicePerkType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** 관리자 매장 상세다. 편집 필드는 수정 요청 본문과 같은 이름이고, AI 요약과 집계는 읽기 전용이다. */
public record MobileAdminShopDetailResult(
        String id,
        String name,
        String branchName,
        String address,
        String region,
        BigDecimal latitude,
        BigDecimal longitude,
        String phone,
        String tagline,
        String description,
        List<String> ramenTypes,
        List<String> tags,
        String instagramUrl,
        String reservationUrl,
        String websiteUrl,
        String naverPlaceId,
        String kakaoPlaceId,
        Integer priceMin,
        Integer priceMax,
        MobileShopBusinessStatus businessStatus,
        String closedDaysText,
        List<String> imageUrls,
        boolean published,
        Instant hoursVerifiedAt,
        List<Hour> businessHours,
        List<Perk> servicePerks,
        String aiReviewSummary,
        List<String> aiSummaryKeywords,
        Instant aiSummaryGeneratedAt,
        int viewCount,
        int logCount,
        int bookmarkCount,
        Instant createdAt,
        Instant updatedAt) {

    /** 시간은 HH:mm이다. 휴무일이면 시간이 모두 null이다. */
    public record Hour(
            int dayOfWeek,
            boolean closed,
            String opensAt,
            String closesAt,
            String breakStart,
            String breakEnd,
            String lastOrderAt) {}

    public record Perk(
            ServicePerkType type, ServicePerkStatus status, Integer price, String condition, Instant verifiedAt) {}
}
