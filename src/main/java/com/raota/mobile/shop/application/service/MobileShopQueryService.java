package com.raota.mobile.shop.application.service;

import com.raota.mobile.common.cursor.Cursor;
import com.raota.mobile.common.cursor.CursorPage;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.shop.application.port.MobileShopBookmarkPort;
import com.raota.mobile.shop.application.port.MobileShopBookmarkPort.SavedShop;
import com.raota.mobile.shop.application.port.MobileShopSearchPort;
import com.raota.mobile.shop.application.port.MobileShopSearchPort.RankedShop;
import com.raota.mobile.shop.application.query.MobileShopSort;
import com.raota.mobile.shop.application.result.MobileShopBusinessHourResult;
import com.raota.mobile.shop.application.result.MobileShopDetail;
import com.raota.mobile.shop.application.result.MobileShopImageResult;
import com.raota.mobile.shop.application.result.MobileShopServicePerkResult;
import com.raota.mobile.shop.application.result.MobileShopMapPin;
import com.raota.mobile.shop.application.result.MobileShopSummary;
import com.raota.mobile.shop.domain.model.MobileShop;
import com.raota.mobile.shop.domain.model.MobileShopBusinessHour;
import com.raota.mobile.shop.domain.model.MobileShopImage;
import com.raota.mobile.shop.domain.repository.MobileShopBusinessHourRepository;
import com.raota.mobile.shop.domain.repository.MobileShopImageRepository;
import com.raota.mobile.shop.domain.repository.MobileShopServicePerkRepository;
import com.raota.mobile.shop.domain.repository.MobileShopRepository;
import com.raota.mobile.shop.domain.service.MobileShopOpenStatus;
import com.raota.mobile.shop.domain.service.MobileShopOpenStatus.Hours;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공개 매장을 조건·정렬별로 읽고 요일별 영업정보를 한 번에 합친다. */
@Service
@RequiredArgsConstructor
public class MobileShopQueryService {

    private static final DateTimeFormatter CLOCK_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final MobileShopSearchPort search;

    private final MobileShopRepository shops;

    private final MobileShopImageRepository images;

    private final MobileShopBusinessHourRepository hours;

    private final MobileShopServicePerkRepository perks;

    private final MobileShopBookmarkPort bookmarks;

    private final MobileShopOpenStatus openStatus;

    @Transactional(readOnly = true)
    public CursorPage<MobileShopSummary> list(Long userId, MobileShopSort sort, String query, String region,
            String ramenType, boolean openNow, BigDecimal latitude, BigDecimal longitude, String rawCursor, int size) {
        validate(sort, latitude, longitude);
        BigDecimal searchLatitude = coordinate(latitude, 90);
        BigDecimal searchLongitude = coordinate(longitude, 180);
        Cursor cursor = Cursor.parse(rawCursor).orElse(null);
        // 확인된 영업시간은 요일·휴식에 따라 달라져 SQL 정렬 이후 일괄 계산한다.
        // 매장은 약 550개라 openNow 요청만 후보 전체를 읽어 필터링한 뒤 페이지를 만든다.
        List<RankedShop> ranked = search.search(sort, query, region, ramenType, searchLatitude, searchLongitude, cursor,
                openNow ? null : size + 1);
        List<Long> ids = ranked.stream().map(RankedShop::id).toList();
        Map<Long, MobileShop> byId = byId(ids);
        Map<Long, List<Hours>> byHours = hours(ids);
        if (openNow) {
            ranked = ranked.stream()
                .filter(row -> Boolean.TRUE.equals(openStatus.isOpen(byId.get(row.id()).getHoursVerifiedAt(),
                        byHours.getOrDefault(row.id(), List.of()))))
                .limit(size + 1L)
                .toList();
        }
        CursorPage<RankedShop> page = CursorPage.of(ranked, size, RankedShop::position);
        List<Long> pageIds = page.items().stream().map(RankedShop::id).toList();
        Map<Long, String> firstImages = images(pageIds);
        var bookmarkedIds = bookmarks.bookmarkedShopIds(userId, pageIds);
        List<MobileShopSummary> items = page.items()
            .stream()
            .map(row -> summary(byId.get(row.id()), firstImages.get(row.id()),
                    byHours.getOrDefault(row.id(), List.of()), row.distance(), bookmarkedIds.contains(row.id())))
            .toList();
        return new CursorPage<>(items, page.nextCursor(), page.hasNext());
    }

    @Transactional(readOnly = true)
    public List<MobileShopMapPin> mapPins() {
        List<MobileShop> visible = shops.findByPublishedTrueAndDeletedAtIsNull();
        Map<Long, List<Hours>> byHours = hours(visible.stream().map(MobileShop::getId).toList());
        return visible.stream()
            .map(shop -> new MobileShopMapPin(shop.getId().toString(), shop.getName(), shop.getBranchName(),
                    shop.getLatitude(), shop.getLongitude(), shop.getRamenTypes(),
                    openStatus.isOpen(shop.getHoursVerifiedAt(), byHours.getOrDefault(shop.getId(), List.of()))))
            .toList();
    }

    @Transactional
    public MobileShopDetail detail(Long userId, Long shopId) {
        MobileShop shop = shops.findByIdAndPublishedTrueAndDeletedAtIsNull(shopId)
            .orElseThrow(() -> new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "매장을 찾을 수 없습니다."));
        List<MobileShopImage> shopImages = images.findByShopIdInOrderBySortOrderAscIdAsc(List.of(shopId));
        List<MobileShopBusinessHour> shopHours = hours.findByShopIdInOrderByDayOfWeekAsc(List.of(shopId));
        List<Hours> openHours = shopHours.stream().map(this::toHours).toList();
        MobileShopSummary base = summary(shop, shopImages.isEmpty() ? null : shopImages.getFirst().getUrl(), openHours,
                null, bookmarks.bookmarkedShopIds(userId, List.of(shopId)).contains(shopId));
        List<MobileShopBusinessHourResult> businessHours = shopHours.stream()
            .map(hour -> new MobileShopBusinessHourResult(hour.getDayOfWeek(), time(hour.getOpensAt()),
                    time(hour.getClosesAt()), time(hour.getBreakStart()), time(hour.getBreakEnd()),
                    time(hour.getLastOrderAt()), hour.isClosed()))
            .toList();
        List<MobileShopServicePerkResult> servicePerks = perks.findByShopIdOrderByIdAsc(shopId)
            .stream()
            .map(perk -> new MobileShopServicePerkResult(perk.getPerkType(), perk.getStatus(), perk.getPrice(),
                    perk.getConditionText(), perk.getVerifiedAt()))
            .toList();
        shops.incrementViewCount(shopId);
        return new MobileShopDetail(base.id(), base.name(), base.branchName(), base.address(), base.region(),
                base.latitude(), base.longitude(), base.imageUrl(), base.tagline(), base.ramenTypes(), base.tags(),
                base.logCount(), base.bookmarkCount(), base.isBookmarked(), base.businessStatus(), base.isOpen(),
                base.distanceMeters(), shop.getDescription(), shop.getPhone(), shop.getInstagramUrl(),
                shop.getReservationUrl(), shop.getWebsiteUrl(), shop.getNaverPlaceId(), shop.getKakaoPlaceId(),
                shop.getPriceMin(), shop.getPriceMax(), shop.getClosedDaysText(), shop.getHoursVerifiedAt(),
                shopImages.stream().map(image -> new MobileShopImageResult(image.getUrl())).toList(), businessHours,
                servicePerks, shop.getAiReviewSummary(), shop.getAiSummaryKeywords(), shop.getAiSummaryGeneratedAt());
    }

    @Transactional(readOnly = true)
    public CursorPage<MobileShopSummary> bookmarked(Long userId, String rawCursor, int size) {
        List<SavedShop> saved = bookmarks.saved(userId, Cursor.parse(rawCursor).orElse(null), size + 1);
        CursorPage<SavedShop> page = CursorPage.of(saved, size, SavedShop::position);
        List<Long> ids = page.items().stream().map(SavedShop::shopId).toList();
        Map<Long, MobileShop> byId = byId(ids);
        Map<Long, String> firstImages = images(ids);
        Map<Long, List<Hours>> byHours = hours(ids);
        List<MobileShopSummary> items = page.items()
            .stream()
            .map(row -> summary(byId.get(row.shopId()), firstImages.get(row.shopId()),
                    byHours.getOrDefault(row.shopId(), List.of()), null, true))
            .toList();
        return new CursorPage<>(items, page.nextCursor(), page.hasNext());
    }

    private Hours toHours(MobileShopBusinessHour hour) {
        return new Hours(hour.getDayOfWeek(), hour.getOpensAt(), hour.getClosesAt(), hour.getBreakStart(),
                hour.getBreakEnd(), hour.isClosed());
    }

    private String time(LocalTime value) {
        return value == null ? null : value.format(CLOCK_TIME);
    }

    private MobileShopSummary summary(MobileShop shop, String imageUrl, List<Hours> shopHours, BigDecimal distance,
            boolean bookmarked) {
        return new MobileShopSummary(shop.getId().toString(), shop.getName(), shop.getBranchName(), shop.getAddress(),
                shop.getRegion(), shop.getLatitude(), shop.getLongitude(), imageUrl, shop.getTagline(),
                shop.getRamenTypes(), shop.getTags(), shop.getLogCount(), shop.getBookmarkCount(), bookmarked,
                shop.getBusinessStatus(), openStatus.isOpen(shop.getHoursVerifiedAt(), shopHours),
                distance == null ? null : distance.setScale(0, RoundingMode.HALF_UP).intValue());
    }

    private Map<Long, MobileShop> byId(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return shops.findAllById(ids).stream().collect(Collectors.toMap(MobileShop::getId, Function.identity()));
    }

    private Map<Long, String> images(List<Long> ids) {
        Map<Long, String> first = new HashMap<>();
        if (!ids.isEmpty()) {
            for (MobileShopImage image : images.findByShopIdInOrderBySortOrderAscIdAsc(ids)) {
                first.putIfAbsent(image.getShopId(), image.getUrl());
            }
        }
        return first;
    }

    private Map<Long, List<Hours>> hours(List<Long> ids) {
        Map<Long, List<Hours>> byShop = new HashMap<>();
        if (!ids.isEmpty()) {
            for (MobileShopBusinessHour hour : hours.findByShopIdInOrderByDayOfWeekAsc(ids)) {
                byShop.computeIfAbsent(hour.getShopId(), ignored -> new ArrayList<>()).add(toHours(hour));
            }
        }
        return byShop;
    }

    private void validate(MobileShopSort sort, BigDecimal latitude, BigDecimal longitude) {
        if (sort == MobileShopSort.DISTANCE || latitude != null || longitude != null) {
            if (latitude == null || longitude == null) {
                throw invalidParams();
            }
        }
    }

    /**
     * 지수 표기로 자릿수가 극단적으로 큰 값은 JDBC가 십진 문자열로 펼치며 메모리를 소모하므로 범위 비교 전에 거절하고, 저장 정밀도인 소수점 8자리로
     * 맞춘다.
     */
    private BigDecimal coordinate(BigDecimal value, int limit) {
        if (value == null) {
            return null;
        }
        if (value.scale() > 20 || value.precision() > 30 || value.abs().compareTo(BigDecimal.valueOf(limit)) > 0) {
            throw invalidParams();
        }
        return value.setScale(8, RoundingMode.HALF_UP);
    }

    private MobileException invalidParams() {
        return new MobileException(MobileErrorCode.VALIDATION_ERROR, "매장 목록 요청 값을 확인해 주세요.");
    }

}
