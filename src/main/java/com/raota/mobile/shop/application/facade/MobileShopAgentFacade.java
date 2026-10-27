package com.raota.mobile.shop.application.facade;

import com.raota.mobile.shop.application.port.MobileShopBookmarkPort;
import com.raota.mobile.shop.application.result.MobileShopSummaryResult;
import com.raota.mobile.shop.application.service.MobileShopRecommendationQueryService;
import com.raota.mobile.shop.domain.model.MobileShop;
import com.raota.mobile.shop.domain.model.MobileShopServicePerk;
import com.raota.mobile.shop.domain.repository.MobileShopRepository;
import com.raota.mobile.shop.domain.repository.MobileShopServicePerkRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** AI 추천·큐레이션·매장 AI 요약(agent)이 쓰는 v2 매장 조회와 요약 저장 API다. */
@Service
@RequiredArgsConstructor
public class MobileShopAgentFacade {

    private final MobileShopRecommendationQueryService recommendationQueryService;

    private final MobileShopRepository shopRepository;

    private final MobileShopServicePerkRepository servicePerkRepository;

    private final MobileShopBookmarkPort bookmarkPort;

    private final ObjectMapper objectMapper;

    /**
     * 추천 후보 매장 ID다. 추천해도 되는 매장 중 라멘 종류가 {@code ramenType}인(null이면 전부) 매장이고, 지역이 오면 그 지역
     * 매장만 남긴다. 좌표가 오면 가까운 순, 없으면 조회수 순으로 최대 {@code limit}곳이다.
     */
    public List<Long> recommendationCandidates(
            String ramenType, String region, BigDecimal latitude, BigDecimal longitude, int limit) {
        return recommendationQueryService.findCandidateIds(ramenType, region, latitude, longitude, limit);
    }

    /** 순위대로 받은 매장 ID 중 추천해도 되는 매장을 최대 {@code limit}개 요약한다. 좌표가 오면 거리를 채운다. */
    public List<MobileShopSummaryResult> recommendable(
            Long viewerId, List<Long> rankedShopIds, BigDecimal latitude, BigDecimal longitude, int limit) {
        if (rankedShopIds.isEmpty()) {
            return List.of();
        }
        return recommendationQueryService.summarizeRecommendable(viewerId, rankedShopIds, latitude, longitude, limit);
    }

    /** 라멘 종류(앱 종류 값 중 하나), 없으면 태그 키워드로 추천해도 되는 매장 중 조회수 1위를 고른다. */
    public Optional<MobileShopSummaryResult> mostPopular(Long viewerId, String ramenType, String tagKeyword) {
        return recommendationQueryService.findMostViewedSummary(viewerId, ramenType, tagKeyword);
    }

    /** 추천 성과 집계용으로 이 회원들이 {@code [from, to)} 사이에 만든 북마크다. 해제한 북마크는 빠진다. */
    @Transactional(readOnly = true)
    public List<MobileShopActivity> bookmarksCreatedBetween(Collection<Long> userIds, Instant from, Instant to) {
        return bookmarkPort.createdBetween(userIds, from, to).stream()
                .map(bookmark -> new MobileShopActivity(bookmark.userId(), bookmark.shopId(), bookmark.createdAt()))
                .toList();
    }

    /**
     * 추천 이유의 근거로 넘길 매장 정보다. 리뷰 AI 요약·매장 설명·가격대와, 무료·유료로 확인된 서비스(면 추가 등)를 담는다. 없는 매장은 빠진다.
     */
    @Transactional(readOnly = true)
    public Map<Long, MobileShopReasonFacts> reasonFacts(Collection<Long> shopIds) {
        Map<Long, List<String>> perksByShop = new HashMap<>();
        for (MobileShopServicePerk perk : servicePerkRepository.findByShopIdInOrderByIdAsc(shopIds)) {
            String label = perkLabel(perk);
            if (label != null) {
                perksByShop
                        .computeIfAbsent(perk.getShopId(), id -> new ArrayList<>())
                        .add(label);
            }
        }
        Map<Long, MobileShopReasonFacts> facts = new HashMap<>();
        for (MobileShop shop : shopRepository.findAllById(shopIds)) {
            facts.put(
                    shop.getId(),
                    new MobileShopReasonFacts(
                            text(shop.getAiReviewSummary()),
                            text(shop.getDescription()),
                            shop.getPriceMin(),
                            shop.getPriceMax(),
                            List.copyOf(perksByShop.getOrDefault(shop.getId(), List.of()))));
        }
        return facts;
    }

    /** AI 요약이 아직 없는 공개 매장 ID를 {@code afterId} 다음부터 ID 순으로 돌려준다. */
    public List<Long> publishedShopIdsWithoutAiSummary(long afterId) {
        return shopRepository.findPublishedIdsWithoutAiSummaryAfter(afterId);
    }

    /** 삭제되지 않은 매장의 AI 요약을 바꾼다. 그런 매장이 없으면 {@code false}다. */
    @Transactional
    public boolean saveAiSummary(Long shopId, String summary, List<String> keywords, Instant generatedAt) {
        return shopRepository.updateAiSummary(shopId, summary, objectMapper.writeValueAsString(keywords), generatedAt)
                == 1;
    }

    private static String text(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /** 제공 여부를 모르거나 제공하지 않는 서비스는 근거가 아니라서 뺀다. */
    private static String perkLabel(MobileShopServicePerk perk) {
        String name = switch (perk.getPerkType()) {
            case NOODLE_REFILL -> "면 추가";
            case RICE_REFILL -> "공깃밥";
            case SOUP_REFILL -> "국물 추가";
            case CONDIMENT -> "추가 양념";
        };
        String label = switch (perk.getStatus()) {
            case FREE -> name + " 무료";
            case PAID -> paidLabel(name, perk.getPrice());
            case NOT_OFFERED, UNKNOWN -> null;
        };
        if (label == null
                || perk.getConditionText() == null
                || perk.getConditionText().isBlank()) {
            return label;
        }
        return label + "(" + perk.getConditionText().trim() + ")";
    }

    private static String paidLabel(String name, Integer price) {
        if (price == null) {
            return name + " 유료";
        }
        return name + " " + price + "원";
    }

    /** 추천 이유를 쓸 때 근거로 삼는 매장 정보다. 값이 없으면 {@code null}이다. */
    public record MobileShopReasonFacts(
            String aiReviewSummary, String description, Integer priceMin, Integer priceMax, List<String> perks) {}

    /** 회원이 매장에 남긴 행동(북마크)과 그 시각이다. */
    public record MobileShopActivity(Long userId, Long shopId, Instant createdAt) {}
}
