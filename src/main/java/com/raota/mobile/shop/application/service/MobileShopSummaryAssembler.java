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
 * 매장을 앱 카드 요약({@code MobileShopSummaryResult})으로 조립한다. 사진·영업시간·북마크는 매장 수와 관계없이 종류마다 한 번에 읽는다.
 * 서비스 안에서만 쓰는 조립기라 트랜잭션을 열지 않고, 부르는 서비스의 트랜잭션을 쓴다.
 *
 * <p>
 * 영업시간이나 북마크 여부를 호출한 쪽이 이미 알면({@code openNow} 필터, 내 북마크 목록) 다시 읽지 않도록 {@link #summarize}에 넘긴다.
 * </p>
 */
@Component
@RequiredArgsConstructor
class MobileShopSummaryAssembler {

    private final MobileShopRepository shops;

    private final MobileShopImageRepository images;

    private final MobileShopBusinessHourRepository hours;

    private final MobileShopBookmarkPort bookmarks;

    private final MobileShopOpenStatus openStatus;

    /**
     * 영업시간과 이 회원의 북마크 여부까지 새로 읽어 받은 순서대로 요약한다. 거리가 없는 매장은 {@code distances}에 넣지 않는다.
     */
    List<MobileShopSummaryResult> loadAndSummarize(
            Long userId, List<MobileShop> orderedShops, Map<Long, BigDecimal> distances) {
        List<Long> ids = orderedShops.stream().map(MobileShop::getId).toList();
        return summarize(orderedShops, findHoursByShopId(ids), bookmarks.bookmarkedShopIds(userId, ids), distances);
    }

    /** 이미 읽은 영업시간·북마크 여부로 받은 순서대로 요약한다. 첫 사진만 여기서 읽는다. */
    List<MobileShopSummaryResult> summarize(
            List<MobileShop> orderedShops,
            Map<Long, List<Hours>> hoursByShopId,
            Set<Long> bookmarkedShopIds,
            Map<Long, BigDecimal> distances) {
        Map<Long, String> firstImageUrls =
                findFirstImageUrls(orderedShops.stream().map(MobileShop::getId).toList());
        return orderedShops.stream()
                .map(shop -> toSummary(
                        shop,
                        firstImageUrls.get(shop.getId()),
                        hoursByShopId.getOrDefault(shop.getId(), List.of()),
                        distances.get(shop.getId()),
                        bookmarkedShopIds.contains(shop.getId())))
                .toList();
    }

    /** 매장 하나를 요약으로 바꾼다. 아무것도 읽지 않는다. */
    MobileShopSummaryResult toSummary(
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
                roundToMeters(distance));
    }

    Map<Long, MobileShop> findShopsById(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return shops.findAllById(ids).stream().collect(Collectors.toMap(MobileShop::getId, Function.identity()));
    }

    Map<Long, List<Hours>> findHoursByShopId(List<Long> ids) {
        Map<Long, List<Hours>> byShopId = new HashMap<>();
        if (!ids.isEmpty()) {
            for (MobileShopBusinessHour hour : hours.findByShopIdInOrderByDayOfWeekAsc(ids)) {
                byShopId.computeIfAbsent(hour.getShopId(), ignored -> new ArrayList<>())
                        .add(toOpenStatusHours(hour));
            }
        }
        return byShopId;
    }

    /** 영업 중 판단({@link MobileShopOpenStatus})에 넘기는 형태로 바꾼다. */
    static Hours toOpenStatusHours(MobileShopBusinessHour hour) {
        return new Hours(
                hour.getDayOfWeek(),
                hour.getOpensAt(),
                hour.getClosesAt(),
                hour.getBreakStart(),
                hour.getBreakEnd(),
                hour.isClosed());
    }

    /** 매장마다 정렬 순서상 첫 사진 주소다. 사진이 없는 매장은 빠진다. 관리자 목록 썸네일도 이걸 쓴다. */
    Map<Long, String> findFirstImageUrls(List<Long> ids) {
        Map<Long, String> first = new HashMap<>();
        if (!ids.isEmpty()) {
            for (MobileShopImage image : images.findByShopIdInOrderBySortOrderAscIdAsc(ids)) {
                first.putIfAbsent(image.getShopId(), image.getUrl());
            }
        }
        return first;
    }

    private static Integer roundToMeters(BigDecimal distance) {
        if (distance == null) {
            return null;
        }
        return distance.setScale(0, RoundingMode.HALF_UP).intValue();
    }
}
