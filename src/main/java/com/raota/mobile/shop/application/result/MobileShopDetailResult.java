package com.raota.mobile.shop.application.result;

import com.raota.mobile.shop.domain.model.MobileShop;
import com.raota.mobile.shop.domain.model.MobileShopBusinessHour;
import com.raota.mobile.shop.domain.model.MobileShopBusinessStatus;
import com.raota.mobile.shop.domain.model.MobileShopImage;
import com.raota.mobile.shop.domain.model.MobileShopServicePerk;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** 한 매장 소개의 모든 확인된 정보와 사진·혜택·요일별 영업시간이다. */
public record MobileShopDetailResult(
        String id,
        String name,
        String branchName,
        String address,
        String region,
        BigDecimal latitude,
        BigDecimal longitude,
        String imageUrl,
        String tagline,
        List<String> ramenTypes,
        List<String> tags,
        int logCount,
        int bookmarkCount,
        BigDecimal averageSatisfaction,
        boolean isBookmarked,
        MobileShopBusinessStatus businessStatus,
        Boolean isOpen,
        Integer distanceMeters,
        String description,
        String phone,
        String instagramUrl,
        String reservationUrl,
        String websiteUrl,
        String naverPlaceId,
        String kakaoPlaceId,
        Integer priceMin,
        Integer priceMax,
        String closedDaysText,
        Instant hoursVerifiedAt,
        List<MobileShopImageResult> images,
        List<MobileShopBusinessHourResult> businessHours,
        List<MobileShopServicePerkResult> servicePerks,
        String aiReviewSummary,
        List<String> aiSummaryKeywords,
        Instant aiSummaryGeneratedAt) {

    /** 카드 요약({@code summary})에 상세에만 있는 매장 정보·사진·영업시간·혜택을 더한다. 사진은 정렬 순서대로다. */
    public static MobileShopDetailResult of(
            MobileShopSummaryResult summary,
            MobileShop shop,
            List<MobileShopImage> shopImages,
            List<MobileShopBusinessHour> shopHours,
            List<MobileShopServicePerk> shopPerks) {
        return new MobileShopDetailResult(
                summary.id(),
                summary.name(),
                summary.branchName(),
                summary.address(),
                summary.region(),
                summary.latitude(),
                summary.longitude(),
                summary.imageUrl(),
                summary.tagline(),
                summary.ramenTypes(),
                summary.tags(),
                summary.logCount(),
                summary.bookmarkCount(),
                shop.averageSatisfaction(),
                summary.isBookmarked(),
                summary.businessStatus(),
                summary.isOpen(),
                summary.distanceMeters(),
                shop.getDescription(),
                shop.getPhone(),
                shop.getInstagramUrl(),
                shop.getReservationUrl(),
                shop.getWebsiteUrl(),
                shop.getNaverPlaceId(),
                shop.getKakaoPlaceId(),
                shop.getPriceMin(),
                shop.getPriceMax(),
                shop.getClosedDaysText(),
                shop.getHoursVerifiedAt(),
                shopImages.stream()
                        .map(image -> new MobileShopImageResult(image.getUrl()))
                        .toList(),
                shopHours.stream().map(MobileShopBusinessHourResult::from).toList(),
                shopPerks.stream().map(MobileShopServicePerkResult::from).toList(),
                shop.getAiReviewSummary(),
                shop.getAiSummaryKeywords(),
                shop.getAiSummaryGeneratedAt());
    }
}
