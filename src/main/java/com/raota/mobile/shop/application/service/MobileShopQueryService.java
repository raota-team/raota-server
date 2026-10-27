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

    private final MobileShopSummaries summaries;

    @Transactional(readOnly = true)
    public CursorPage<MobileShopSummaryResult> list(
            Long userId, MobileShopSearchCondition condition, String rawCursor, int size) {
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
        return new CursorPage<>(summarize(userId, page.items(), ranked), page.nextCursor(), page.hasNext());
    }

    /**
     * 지도 탭은 목록과 핀을 같은 매장 집합으로 그리므로 조건에 맞는 공개 매장을 페이지 없이 모두 준다. 정렬·필터는 {@link #list}와 같다.
     * 공개 매장이 수백 곳이라 한 번에 보낸다.
     */
    @Transactional(readOnly = true)
    public List<MobileShopSummaryResult> all(Long userId, MobileShopSearchCondition condition) {
        Ranked ranked = rank(condition, null, null);
        return summarize(userId, ranked.rows(), ranked);
    }

    /** 영업 중 필터에 쓴 매장·영업시간을 요약에도 그대로 쓴다. */
    private Ranked rank(MobileShopSearchCondition condition, Cursor cursor, Integer limit) {
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
        Map<Long, MobileShop> byId = summaries.byId(ids);
        Map<Long, List<Hours>> byHours = summaries.hoursByShop(ids);
        if (condition.openNow()) {
            rows = rows.stream()
                    .filter(row -> Boolean.TRUE.equals(openStatus.isOpen(
                            byId.get(row.id()).getHoursVerifiedAt(), byHours.getOrDefault(row.id(), List.of()))))
                    .toList();
        }
        return new Ranked(rows, byId, byHours);
    }

    private List<MobileShopSummaryResult> summarize(Long userId, List<RankedShop> rows, Ranked ranked) {
        List<Long> ids = rows.stream().map(RankedShop::id).toList();
        Map<Long, BigDecimal> distances = new HashMap<>();
        for (RankedShop row : rows) {
            if (row.distance() != null) {
                distances.put(row.id(), row.distance());
            }
        }
        return summaries.summarize(
                ids.stream().map(ranked.byId()::get).toList(),
                ranked.byHours(),
                bookmarks.bookmarkedShopIds(userId, ids),
                distances);
    }

    private record Ranked(List<RankedShop> rows, Map<Long, MobileShop> byId, Map<Long, List<Hours>> byHours) {}

    /** 상세를 열면 조회수를 원자적으로 하나 올린다. */
    @Transactional
    public MobileShopDetailResult detail(Long userId, Long shopId) {
        MobileShop shop = shops.findByIdAndPublishedTrueAndDeletedAtIsNull(shopId)
                .orElseThrow(() -> new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "매장을 찾을 수 없습니다."));
        List<MobileShopImage> shopImages = images.findByShopIdInOrderBySortOrderAscIdAsc(List.of(shopId));
        List<MobileShopBusinessHour> shopHours = hours.findByShopIdInOrderByDayOfWeekAsc(List.of(shopId));
        String imageUrl = null;
        if (!shopImages.isEmpty()) {
            imageUrl = shopImages.getFirst().getUrl();
        }
        MobileShopSummaryResult summary = summaries.summary(
                shop,
                imageUrl,
                shopHours.stream().map(MobileShopSummaries::toHours).toList(),
                null,
                bookmarks.bookmarkedShopIds(userId, List.of(shopId)).contains(shopId));
        MobileShopDetailResult detail =
                MobileShopDetailResult.of(summary, shop, shopImages, shopHours, perks.findByShopIdOrderByIdAsc(shopId));
        shops.incrementViewCount(shopId);
        return detail;
    }

    /** 북마크한 매장은 모두 저장된 상태라 북마크 여부를 다시 읽지 않는다. */
    @Transactional(readOnly = true)
    public CursorPage<MobileShopSummaryResult> bookmarked(Long userId, String rawCursor, int size) {
        List<SavedShop> saved = bookmarks.saved(userId, Cursor.parse(rawCursor).orElse(null), size + 1);
        CursorPage<SavedShop> page = CursorPage.of(saved, size, SavedShop::position);
        List<Long> ids = page.items().stream().map(SavedShop::shopId).toList();
        Map<Long, MobileShop> byId = summaries.byId(ids);
        List<MobileShopSummaryResult> items = summaries.summarize(
                ids.stream().map(byId::get).toList(), summaries.hoursByShop(ids), Set.copyOf(ids), Map.of());
        return new CursorPage<>(items, page.nextCursor(), page.hasNext());
    }
}
