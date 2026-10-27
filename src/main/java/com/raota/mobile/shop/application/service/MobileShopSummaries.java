package com.raota.mobile.shop.application.service;

import com.raota.mobile.shop.application.port.MobileShopBookmarkPort;
import com.raota.mobile.shop.application.result.MobileShopSummaryResult;
import com.raota.mobile.shop.domain.model.MobileShop;
import com.raota.mobile.shop.domain.model.MobileShopBusinessHour;
import com.raota.mobile.shop.domain.model.MobileShopImage;
import com.raota.mobile.shop.domain.repository.MobileShopBusinessHourRepository;
import com.raota.mobile.shop.domain.repository.MobileShopImageRepository;
import com.raota.mobile.shop.domain.repository.MobileShopRepository;
import com.raota.mobile.shop.domain.service.MobileShopOpenStatus;
import com.raota.mobile.shop.domain.service.MobileShopOpenStatus.Hours;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 매장을 앱 카드 요약으로 바꾼다. 사진·영업시간·북마크는 매장 수와 관계없이 종류마다 한 번에 읽는다.
 *
 * <p>
 * 영업시간이나 북마크 여부를 호출한 쪽이 이미 알면({@code openNow} 필터, 내 북마크 목록) 다시 읽지 않도록 {@link #summarize}에 넘긴다.
 * </p>
 */
@Component
@RequiredArgsConstructor
class MobileShopSummaries {

    private final MobileShopRepository shops;

    private final MobileShopImageRepository images;

    private final MobileShopBusinessHourRepository hours;

    private final MobileShopBookmarkPort bookmarks;

    private final MobileShopOpenStatus openStatus;

    /** 영업시간과 북마크 여부까지 읽어 받은 순서대로 요약한다. 거리가 없는 매장은 {@code distances}에 넣지 않는다. */
    List<MobileShopSummaryResult> load(Long userId, List<MobileShop> ordered, Map<Long, BigDecimal> distances) {
        List<Long> ids = ordered.stream().map(MobileShop::getId).toList();
        return summarize(ordered, hoursByShop(ids), bookmarks.bookmarkedShopIds(userId, ids), distances);
    }

    /** 받은 순서대로 요약한다. 첫 사진만 여기서 읽는다. */
    List<MobileShopSummaryResult> summarize(
            List<MobileShop> ordered,
            Map<Long, List<Hours>> hoursByShop,
            Set<Long> bookmarkedIds,
            Map<Long, BigDecimal> distances) {
        Map<Long, String> firstImages =
                firstImages(ordered.stream().map(MobileShop::getId).toList());
        return ordered.stream()
                .map(shop -> summary(
                        shop,
                        firstImages.get(shop.getId()),
                        hoursByShop.getOrDefault(shop.getId(), List.of()),
                        distances.get(shop.getId()),
                        bookmarkedIds.contains(shop.getId())))
                .toList();
    }

    MobileShopSummaryResult summary(
            MobileShop shop, String imageUrl, List<Hours> shopHours, BigDecimal distance, boolean bookmarked) {
        return new MobileShopSummaryResult(
                shop.getId().toString(),
                shop.getName(),
                shop.getBranchName(),
                shop.getAddress(),
                shop.getRegion(),
                shop.getLatitude(),
                shop.getLongitude(),
                imageUrl,
                shop.getTagline(),
                shop.getRamenTypes(),
                shop.getTags(),
                shop.getLogCount(),
                shop.getBookmarkCount(),
                bookmarked,
                shop.getBusinessStatus(),
                openStatus.isOpen(shop.getHoursVerifiedAt(), shopHours),
                roundedMeters(distance));
    }

    Map<Long, MobileShop> byId(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return shops.findAllById(ids).stream().collect(Collectors.toMap(MobileShop::getId, Function.identity()));
    }

    Map<Long, List<Hours>> hoursByShop(List<Long> ids) {
        Map<Long, List<Hours>> byShop = new HashMap<>();
        if (!ids.isEmpty()) {
            for (MobileShopBusinessHour hour : hours.findByShopIdInOrderByDayOfWeekAsc(ids)) {
                byShop.computeIfAbsent(hour.getShopId(), ignored -> new ArrayList<>())
                        .add(toHours(hour));
            }
        }
        return byShop;
    }

    static Hours toHours(MobileShopBusinessHour hour) {
        return new Hours(
                hour.getDayOfWeek(),
                hour.getOpensAt(),
                hour.getClosesAt(),
                hour.getBreakStart(),
                hour.getBreakEnd(),
                hour.isClosed());
    }

    private Map<Long, String> firstImages(List<Long> ids) {
        Map<Long, String> first = new HashMap<>();
        if (!ids.isEmpty()) {
            for (MobileShopImage image : images.findByShopIdInOrderBySortOrderAscIdAsc(ids)) {
                first.putIfAbsent(image.getShopId(), image.getUrl());
            }
        }
        return first;
    }

    private static Integer roundedMeters(BigDecimal distance) {
        if (distance == null) {
            return null;
        }
        return distance.setScale(0, RoundingMode.HALF_UP).intValue();
    }
}
