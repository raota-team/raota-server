package com.raota.mobile.shop.application.service;

import com.raota.mobile.shop.application.result.MobileShopSummaryResult;
import com.raota.mobile.shop.domain.model.MobileShop;
import com.raota.mobile.shop.domain.repository.MobileShopRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** AI 추천·오늘의 큐레이션(agent)만 쓰는 매장 조회다. {@code MobileShopAgentFacade}를 거쳐서만 부른다. */
@Service
@RequiredArgsConstructor
public class MobileShopRecommendationQueryService {

    private final MobileShopRepository shops;

    private final MobileShopSummaries summaries;

    /**
     * 추천 후보 ID다. 추천해도 되는 매장 중 라멘 종류가 {@code ramenType}인 매장(null이면 전부)이고, 지역이 오면 지역(구·시)이나
     * 주소에 그 지역이 들어간 매장만 남긴다. 좌표가 오면 좌표가 있는 매장을 가까운 순으로, 없으면 조회수 순으로 최대 {@code limit}곳이다.
     * 반경은 없다.
     */
    @Transactional(readOnly = true)
    public List<Long> recommendationCandidates(
            String ramenType, String region, BigDecimal latitude, BigDecimal longitude, int limit) {
        List<Long> ids = shops.findRecommendableIds(ramenType);
        Map<Long, MobileShop> byId = summaries.byId(ids);
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
    public List<MobileShopSummaryResult> recommendable(
            Long userId, List<Long> rankedIds, BigDecimal latitude, BigDecimal longitude, int limit) {
        Map<Long, MobileShop> byId = summaries.byId(rankedIds);
        List<MobileShop> picked = new ArrayList<>(limit);
        Map<Long, BigDecimal> distances = new HashMap<>();
        for (Long id : rankedIds) {
            MobileShop shop = byId.get(id);
            if (shop == null || !shop.isRecommendable()) {
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
        return summaries.load(userId, picked, distances);
    }

    /**
     * 추천해도 되는 매장 중 라멘 종류가 {@code ramenType}인 매장, 종류가 없으면 태그에 {@code tagKeyword}가 들어간 매장에서
     * 조회수가 가장 많은 매장을 고른다. 조회수가 같으면 ID가 큰 매장이다. 공개 API라 DB에서 1건만 읽는다.
     */
    @Transactional(readOnly = true)
    public Optional<MobileShopSummaryResult> mostPopular(Long userId, String ramenType, String tagKeyword) {
        return shops.findMostViewedRecommendableId(ramenType, tagKeyword)
                .flatMap(shops::findById)
                .map(shop -> summaries.load(userId, List.of(shop), Map.of()).getFirst());
    }

    private static boolean inRegion(MobileShop shop, String region) {
        if (region == null) {
            return true;
        }
        return region.equals(shop.getRegion())
                || (shop.getAddress() != null && shop.getAddress().contains(region));
    }

    /** {@code MobileShopJdbcSearch.HAVERSINE}과 같은 하버사인 거리(지구 반지름 6,371km)다. 바꾸면 둘 다 바꾼다. */
    private static double distanceMeters(
            BigDecimal fromLatitude, BigDecimal fromLongitude, BigDecimal toLatitude, BigDecimal toLongitude) {
        double lat1 = Math.toRadians(fromLatitude.doubleValue());
        double lat2 = Math.toRadians(toLatitude.doubleValue());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(toLongitude.doubleValue() - fromLongitude.doubleValue());
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLng / 2), 2);
        return 6371000 * 2 * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}
