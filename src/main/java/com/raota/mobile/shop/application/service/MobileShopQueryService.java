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
import com.raota.mobile.shop.application.result.MobileShopDetailResult;
import com.raota.mobile.shop.application.result.MobileShopSummaryResult;
import com.raota.mobile.shop.domain.model.MobileShop;
import com.raota.mobile.shop.domain.model.MobileShopBusinessHour;
import com.raota.mobile.shop.domain.model.MobileShopImage;
import com.raota.mobile.shop.domain.repository.MobileShopBusinessHourRepository;
import com.raota.mobile.shop.domain.repository.MobileShopImageRepository;
import com.raota.mobile.shop.domain.repository.MobileShopRepository;
import com.raota.mobile.shop.domain.repository.MobileShopServicePerkRepository;
import com.raota.mobile.shop.domain.service.MobileShopOpenStatus;
import com.raota.mobile.shop.domain.service.MobileShopOpenStatus.Hours;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 앱의 공개 매장 목록·지도·상세와 내 북마크 목록이다. agent용 조회는 {@link MobileShopRecommendationQueryService}에 있다. */
@Service
@RequiredArgsConstructor
public class MobileShopQueryService {

    private final MobileShopSearchPort search;

    private final MobileShopRepository shops;

    private final MobileShopImageRepository images;

    private final MobileShopBusinessHourRepository hours;

    private final MobileShopServicePerkRepository perks;

    private final MobileShopBookmarkPort bookmarks;

    private final MobileShopOpenStatus openStatus;

    private final MobileShopSummaryAssembler assembler;

    /** 매장 목록 탭이다. 조건에 맞는 공개 매장을 정렬 순서대로 커서 페이지로 준다. */
    @Transactional(readOnly = true)
    public CursorPage<MobileShopSummaryResult> searchPage(
            Long userId, MobileShopSearchCondition condition, String rawCursor, int size) {
        Cursor cursor = Cursor.parse(rawCursor).orElse(null);
        // 확인된 영업시간은 요일·휴식에 따라 달라져 SQL 정렬 이후 일괄 계산한다.
        // 매장은 약 550개라 openNow 요청만 후보 전체를 읽어 필터링한 뒤 페이지를 만든다.
        Integer limit = size + 1;
        if (condition.openNow()) {
            limit = null;
        }
        RankedShops ranked = searchRanked(condition, cursor, limit);
        CursorPage<RankedShop> page =
                CursorPage.of(ranked.rows().stream().limit(size + 1L).toList(), size, RankedShop::position);
        return new CursorPage<>(toSummaries(userId, page.items(), ranked), page.nextCursor(), page.hasNext());
    }

    /**
     * 지도 탭은 목록과 핀을 같은 매장 집합으로 그리므로 조건에 맞는 공개 매장을 페이지 없이 모두 준다. 정렬·필터는 {@link #searchPage}와
     * 같다.
     * 공개 매장이 수백 곳이라 한 번에 보낸다.
     */
    @Transactional(readOnly = true)
    public List<MobileShopSummaryResult> searchAll(Long userId, MobileShopSearchCondition condition) {
        RankedShops ranked = searchRanked(condition, null, null);
        return toSummaries(userId, ranked.rows(), ranked);
    }

    /**
     * 정렬된 매장 순위를 읽고, 그 매장들과 영업시간도 함께 읽어 둔다. 영업 중 필터에 쓴 매장·영업시간을 요약에도 그대로 쓰려는 것이다.
     */
    private RankedShops searchRanked(MobileShopSearchCondition condition, Cursor cursor, Integer limit) {
        List<RankedShop> rows = search.search(
                condition.sort(),
                condition.query(),
                condition.region(),
                condition.ramenType(),
                condition.latitude(),
                condition.longitude(),
                cursor,
                limit);
        List<Long> ids = rows.stream().map(RankedShop::id).toList();
        Map<Long, MobileShop> shopsById = assembler.findShopsById(ids);
        Map<Long, List<Hours>> hoursByShopId = assembler.findHoursByShopId(ids);
        if (condition.openNow()) {
            rows = rows.stream()
                    .filter(row -> Boolean.TRUE.equals(openStatus.isOpen(
                            shopsById.get(row.id()).getHoursVerifiedAt(),
                            hoursByShopId.getOrDefault(row.id(), List.of()))))
                    .toList();
        }
        return new RankedShops(rows, shopsById, hoursByShopId);
    }

    /** 순위 행을 요약으로 바꾼다. 매장·영업시간은 이미 읽었으므로 북마크 여부만 새로 읽는다. */
    private List<MobileShopSummaryResult> toSummaries(Long userId, List<RankedShop> rows, RankedShops ranked) {
        List<Long> ids = rows.stream().map(RankedShop::id).toList();
        Map<Long, BigDecimal> distances = new HashMap<>();
        for (RankedShop row : rows) {
            if (row.distance() != null) {
                distances.put(row.id(), row.distance());
            }
        }
        return assembler.summarize(
                ids.stream().map(ranked.shopsById()::get).toList(),
                ranked.hoursByShopId(),
                bookmarks.bookmarkedShopIds(userId, ids),
                distances);
    }

    /** 정렬된 순위 행과, 그 매장·영업시간을 매장 ID로 찾아 둔 것이다. */
    private record RankedShops(
            List<RankedShop> rows, Map<Long, MobileShop> shopsById, Map<Long, List<Hours>> hoursByShopId) {}

    /** 매장 상세를 보여 주고 조회수를 원자적으로 하나 올린다. */
    @Transactional
    public MobileShopDetailResult viewDetail(Long userId, Long shopId) {
        MobileShop shop = shops.findByIdAndPublishedTrueAndDeletedAtIsNull(shopId)
                .orElseThrow(() -> new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "매장을 찾을 수 없습니다."));
        List<MobileShopImage> shopImages = images.findByShopIdInOrderBySortOrderAscIdAsc(List.of(shopId));
        List<MobileShopBusinessHour> shopHours = hours.findByShopIdInOrderByDayOfWeekAsc(List.of(shopId));
        String imageUrl = null;
        if (!shopImages.isEmpty()) {
            imageUrl = shopImages.getFirst().getUrl();
        }
        MobileShopSummaryResult summary = assembler.toSummary(
                shop,
                imageUrl,
                shopHours.stream()
                        .map(MobileShopSummaryAssembler::toOpenStatusHours)
                        .toList(),
                null,
                bookmarks.bookmarkedShopIds(userId, List.of(shopId)).contains(shopId));
        MobileShopDetailResult detail =
                MobileShopDetailResult.of(summary, shop, shopImages, shopHours, perks.findByShopIdOrderByIdAsc(shopId));
        shops.incrementViewCount(shopId);
        return detail;
    }

    /** 내 북마크 목록이다. 북마크한 매장은 모두 저장된 상태라 북마크 여부를 다시 읽지 않는다. */
    @Transactional(readOnly = true)
    public CursorPage<MobileShopSummaryResult> listBookmarked(Long userId, String rawCursor, int size) {
        List<SavedShop> saved = bookmarks.saved(userId, Cursor.parse(rawCursor).orElse(null), size + 1);
        CursorPage<SavedShop> page = CursorPage.of(saved, size, SavedShop::position);
        List<Long> ids = page.items().stream().map(SavedShop::shopId).toList();
        Map<Long, MobileShop> shopsById = assembler.findShopsById(ids);
        List<MobileShopSummaryResult> items = assembler.summarize(
                ids.stream().map(shopsById::get).toList(), assembler.findHoursByShopId(ids), Set.copyOf(ids), Map.of());
        return new CursorPage<>(items, page.nextCursor(), page.hasNext());
    }
}
