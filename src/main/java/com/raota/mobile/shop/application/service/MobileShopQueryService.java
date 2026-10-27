package com.raota.mobile.shop.application.service;

import com.raota.mobile.common.cursor.Cursor;
import com.raota.mobile.common.cursor.CursorPage;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.shop.application.port.MobileShopBookmarkPort;
import com.raota.mobile.shop.application.port.MobileShopBookmarkPort.SavedShop;
import com.raota.mobile.shop.application.port.MobileShopSearchPort;
import com.raota.mobile.shop.application.port.MobileShopSearchPort.RankedShop;
import com.raota.mobile.shop.application.query.MobileShopSearchCondition;
import com.raota.mobile.shop.application.query.MobileShopSort;
import com.raota.mobile.shop.application.result.MobileShopBusinessHourResult;
import com.raota.mobile.shop.application.result.MobileShopDetail;
import com.raota.mobile.shop.application.result.MobileShopImageResult;
import com.raota.mobile.shop.application.result.MobileShopServicePerkResult;
import com.raota.mobile.shop.application.result.MobileShopSummary;
import com.raota.mobile.shop.domain.model.MobileShop;
import com.raota.mobile.shop.domain.model.MobileShopBusinessHour;
import com.raota.mobile.shop.domain.model.MobileShopBusinessStatus;
import com.raota.mobile.shop.domain.model.MobileShopImage;
import com.raota.mobile.shop.domain.repository.MobileShopBusinessHourRepository;
import com.raota.mobile.shop.domain.repository.MobileShopImageRepository;
import com.raota.mobile.shop.domain.repository.MobileShopRepository;
import com.raota.mobile.shop.domain.repository.MobileShopServicePerkRepository;
import com.raota.mobile.shop.domain.service.MobileShopOpenStatus;
import com.raota.mobile.shop.domain.service.MobileShopOpenStatus.Hours;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public CursorPage<MobileShopSummary> list(
            Long userId, MobileShopSearchCondition condition, String rawCursor, int size) {
        validate(condition.sort(), condition.latitude(), condition.longitude());
        Cursor cursor = Cursor.parse(rawCursor).orElse(null);
        // 확인된 영업시간은 요일·휴식에 따라 달라져 SQL 정렬 이후 일괄 계산한다.
        // 매장은 약 550개라 openNow 요청만 후보 전체를 읽어 필터링한 뒤 페이지를 만든다.
        Integer limit = size + 1;
        if (condition.openNow()) {
            limit = null;
        }
        Ranked ranked = rank(condition, cursor, limit);
        CursorPage<RankedShop> page =
                CursorPage.of(ranked.rows().stream().limit(size + 1L).toList(), size, RankedShop::position);
        return new CursorPage<>(summaries(userId, page.items(), ranked), page.nextCursor(), page.hasNext());
    }

    /**
     * 지도 탭은 목록과 핀을 같은 매장 집합으로 그리므로 조건에 맞는 공개 매장을 페이지 없이 모두 준다. 정렬·필터는 {@link #list}와 같다.
     * 공개 매장이 수백 곳이라 한 번에 보낸다.
     */
    @Transactional(readOnly = true)
    public List<MobileShopSummary> all(Long userId, MobileShopSearchCondition condition) {
        validate(condition.sort(), condition.latitude(), condition.longitude());
        Ranked ranked = rank(condition, null, null);
        return summaries(userId, ranked.rows(), ranked);
    }

    private Ranked rank(MobileShopSearchCondition condition, Cursor cursor, Integer limit) {
        List<RankedShop> rows = search.search(
                condition.sort(),
                condition.query(),
                condition.region(),
                condition.ramenType(),
                coordinate(condition.latitude(), 90),
                coordinate(condition.longitude(), 180),
                cursor,
                limit);
        List<Long> ids = rows.stream().map(RankedShop::id).toList();
        Map<Long, MobileShop> byId = byId(ids);
        Map<Long, List<Hours>> byHours = hours(ids);
        if (condition.openNow()) {
            rows = rows.stream()
                    .filter(row -> Boolean.TRUE.equals(openStatus.isOpen(
                            byId.get(row.id()).getHoursVerifiedAt(), byHours.getOrDefault(row.id(), List.of()))))
                    .toList();
        }
        return new Ranked(rows, byId, byHours);
    }

    private List<MobileShopSummary> summaries(Long userId, List<RankedShop> rows, Ranked ranked) {
        List<Long> ids = rows.stream().map(RankedShop::id).toList();
        Map<Long, String> firstImages = images(ids);
        var bookmarkedIds = bookmarks.bookmarkedShopIds(userId, ids);
        return rows.stream()
                .map(row -> summary(
                        ranked.byId().get(row.id()),
                        firstImages.get(row.id()),
                        ranked.byHours().getOrDefault(row.id(), List.of()),
                        row.distance(),
                        bookmarkedIds.contains(row.id())))
                .toList();
    }

    private record Ranked(List<RankedShop> rows, Map<Long, MobileShop> byId, Map<Long, List<Hours>> byHours) {}

    @Transactional
    public MobileShopDetail detail(Long userId, Long shopId) {
        MobileShop shop = shops.findByIdAndPublishedTrueAndDeletedAtIsNull(shopId)
                .orElseThrow(() -> new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "매장을 찾을 수 없습니다."));
        List<MobileShopImage> shopImages = images.findByShopIdInOrderBySortOrderAscIdAsc(List.of(shopId));
        List<MobileShopBusinessHour> shopHours = hours.findByShopIdInOrderByDayOfWeekAsc(List.of(shopId));
        List<Hours> openHours = shopHours.stream().map(this::toHours).toList();
        String imageUrl = null;
        if (!shopImages.isEmpty()) {
            imageUrl = shopImages.getFirst().getUrl();
        }
        MobileShopSummary base = summary(
                shop,
                imageUrl,
                openHours,
                null,
                bookmarks.bookmarkedShopIds(userId, List.of(shopId)).contains(shopId));
        List<MobileShopBusinessHourResult> businessHours = shopHours.stream()
                .map(hour -> new MobileShopBusinessHourResult(
                        hour.getDayOfWeek(),
                        time(hour.getOpensAt()),
                        time(hour.getClosesAt()),
                        time(hour.getBreakStart()),
                        time(hour.getBreakEnd()),
                        time(hour.getLastOrderAt()),
                        hour.isClosed()))
                .toList();
        List<MobileShopServicePerkResult> servicePerks = perks.findByShopIdOrderByIdAsc(shopId).stream()
                .map(perk -> new MobileShopServicePerkResult(
                        perk.getPerkType(),
                        perk.getStatus(),
                        perk.getPrice(),
                        perk.getConditionText(),
                        perk.getVerifiedAt()))
                .toList();
        shops.incrementViewCount(shopId);
        return new MobileShopDetail(
                base.id(),
                base.name(),
                base.branchName(),
                base.address(),
                base.region(),
                base.latitude(),
                base.longitude(),
                base.imageUrl(),
                base.tagline(),
                base.ramenTypes(),
                base.tags(),
                base.logCount(),
                base.bookmarkCount(),
                averageSatisfaction(shop),
                base.isBookmarked(),
                base.businessStatus(),
                base.isOpen(),
                base.distanceMeters(),
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
                businessHours,
                servicePerks,
                shop.getAiReviewSummary(),
                shop.getAiSummaryKeywords(),
                shop.getAiSummaryGeneratedAt());
    }

    private BigDecimal averageSatisfaction(MobileShop shop) {
        if (shop.getScoredLogCount() < 3) {
            return null;
        }
        return BigDecimal.valueOf(shop.getSatisfactionScoreSum())
                .divide(BigDecimal.valueOf(shop.getScoredLogCount()), 1, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public CursorPage<MobileShopSummary> bookmarked(Long userId, String rawCursor, int size) {
        List<SavedShop> saved = bookmarks.saved(userId, Cursor.parse(rawCursor).orElse(null), size + 1);
        CursorPage<SavedShop> page = CursorPage.of(saved, size, SavedShop::position);
        List<Long> ids = page.items().stream().map(SavedShop::shopId).toList();
        Map<Long, MobileShop> byId = byId(ids);
        Map<Long, String> firstImages = images(ids);
        Map<Long, List<Hours>> byHours = hours(ids);
        List<MobileShopSummary> items = page.items().stream()
                .map(row -> summary(
                        byId.get(row.shopId()),
                        firstImages.get(row.shopId()),
                        byHours.getOrDefault(row.shopId(), List.of()),
                        null,
                        true))
                .toList();
        return new CursorPage<>(items, page.nextCursor(), page.hasNext());
    }

    /**
     * 추천 후보 ID다. 추천해도 되는 매장 중 라멘 종류가 {@code ramenType}인 매장(null이면 전부)이고, 지역이 오면 지역(구·시)이나
     * 주소에 그 지역이 들어간 매장만 남긴다. 좌표가 오면 좌표가 있는 매장을 가까운 순으로, 없으면 조회수 순으로 최대 {@code limit}곳이다.
     * 반경은 없다.
     */
    @Transactional(readOnly = true)
    public List<Long> recommendationCandidates(
            String ramenType, String region, BigDecimal latitude, BigDecimal longitude, int limit) {
        List<Long> ids = shops.findRecommendableIds(ramenType);
        Map<Long, MobileShop> byId = byId(ids);
        List<MobileShop> candidates = ids.stream()
                .map(byId::get)
                .filter(shop -> shop != null && inRegion(shop, region))
                .toList();
        if (latitude == null) {
            return candidates.stream().limit(limit).map(MobileShop::getId).toList();
        }
        return candidates.stream()
                .filter(shop -> shop.getLatitude() != null && shop.getLongitude() != null)
                .sorted(Comparator.comparingDouble((MobileShop shop) ->
                                distanceMeters(latitude, longitude, shop.getLatitude(), shop.getLongitude()))
                        .thenComparing(MobileShop::getId))
                .limit(limit)
                .map(MobileShop::getId)
                .toList();
    }

    /**
     * 순위대로 받은 매장 중 지금 추천해도 되는 매장을 최대 {@code limit}개 요약한다. 숨김·삭제·폐업·휴업 매장은 빼고 순서는 받은 순위를
     * 따른다. 좌표가 오면 거리를 채운다.
     */
    @Transactional(readOnly = true)
    public List<MobileShopSummary> recommendable(
            Long userId, List<Long> rankedIds, BigDecimal latitude, BigDecimal longitude, int limit) {
        Map<Long, MobileShop> byId = byId(rankedIds);
        List<MobileShop> picked = new ArrayList<>(limit);
        Map<Long, BigDecimal> distances = new HashMap<>();
        for (Long id : rankedIds) {
            MobileShop shop = byId.get(id);
            if (shop == null || !isRecommendable(shop)) {
                continue;
            }
            if (latitude != null && shop.getLatitude() != null && shop.getLongitude() != null) {
                distances.put(
                        id,
                        BigDecimal.valueOf(
                                distanceMeters(latitude, longitude, shop.getLatitude(), shop.getLongitude())));
            }
            picked.add(shop);
            if (picked.size() == limit) {
                break;
            }
        }
        List<Long> ids = picked.stream().map(MobileShop::getId).toList();
        Map<Long, String> firstImages = images(ids);
        Map<Long, List<Hours>> byHours = hours(ids);
        var bookmarkedIds = bookmarks.bookmarkedShopIds(userId, ids);
        return picked.stream()
                .map(shop -> summary(
                        shop,
                        firstImages.get(shop.getId()),
                        byHours.getOrDefault(shop.getId(), List.of()),
                        distances.get(shop.getId()),
                        bookmarkedIds.contains(shop.getId())))
                .toList();
    }

    /**
     * 추천해도 되는 매장 중 라멘 종류가 {@code ramenType}인 매장, 종류가 없으면 태그에 {@code tagKeyword}가 들어간 매장에서
     * 조회수가 가장 많은 매장을 고른다. 조회수가 같으면 ID가 큰 매장이다. 공개 API라 DB에서 1건만 읽는다.
     */
    @Transactional(readOnly = true)
    public Optional<MobileShopSummary> mostPopular(Long userId, String ramenType, String tagKeyword) {
        Optional<MobileShop> best =
                shops.findMostViewedRecommendableId(ramenType, tagKeyword).flatMap(shops::findById);
        if (best.isEmpty()) {
            return Optional.empty();
        }
        MobileShop shop = best.get();
        List<Long> ids = List.of(shop.getId());
        return Optional.of(summary(
                shop,
                images(ids).get(shop.getId()),
                hours(ids).getOrDefault(shop.getId(), List.of()),
                null,
                bookmarks.bookmarkedShopIds(userId, ids).contains(shop.getId())));
    }

    private boolean isRecommendable(MobileShop shop) {
        return shop.isPublished()
                && shop.getDeletedAt() == null
                && shop.getBusinessStatus() != MobileShopBusinessStatus.CLOSED
                && shop.getBusinessStatus() != MobileShopBusinessStatus.TEMPORARILY_CLOSED;
    }

    private boolean inRegion(MobileShop shop, String region) {
        if (region == null) {
            return true;
        }
        return region.equals(shop.getRegion())
                || (shop.getAddress() != null && shop.getAddress().contains(region));
    }

    /** 매장 목록 거리순 SQL과 같은 하버사인 거리(지구 반지름 6,371km)다. */
    private static double distanceMeters(
            BigDecimal fromLatitude, BigDecimal fromLongitude, BigDecimal toLatitude, BigDecimal toLongitude) {
        double lat1 = Math.toRadians(fromLatitude.doubleValue());
        double lat2 = Math.toRadians(toLatitude.doubleValue());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(toLongitude.doubleValue() - fromLongitude.doubleValue());
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLng / 2), 2);
        return 6371000 * 2 * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    private Hours toHours(MobileShopBusinessHour hour) {
        return new Hours(
                hour.getDayOfWeek(),
                hour.getOpensAt(),
                hour.getClosesAt(),
                hour.getBreakStart(),
                hour.getBreakEnd(),
                hour.isClosed());
    }

    private String time(LocalTime value) {
        if (value == null) {
            return null;
        }
        return value.format(CLOCK_TIME);
    }

    private MobileShopSummary summary(
            MobileShop shop, String imageUrl, List<Hours> shopHours, BigDecimal distance, boolean bookmarked) {
        return new MobileShopSummary(
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

    private Integer roundedMeters(BigDecimal distance) {
        if (distance == null) {
            return null;
        }
        return distance.setScale(0, RoundingMode.HALF_UP).intValue();
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
                byShop.computeIfAbsent(hour.getShopId(), ignored -> new ArrayList<>())
                        .add(toHours(hour));
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
