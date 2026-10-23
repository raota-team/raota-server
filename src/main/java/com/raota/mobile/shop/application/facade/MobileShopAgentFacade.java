package com.raota.mobile.shop.application.facade;

import com.raota.mobile.shop.application.result.MobileShopSummary;
import com.raota.mobile.shop.application.service.MobileShopQueryService;
import com.raota.mobile.shop.domain.model.MobileShop;
import com.raota.mobile.shop.domain.repository.MobileShopRepository;
import java.math.BigDecimal;
import java.time.Instant;
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

    private final MobileShopQueryService shops;

    private final MobileShopRepository repository;

    private final ObjectMapper mapper;

    /**
     * 추천 후보 매장 ID다. 추천해도 되는 매장 중 라멘 종류가 {@code ramenType}인(null이면 전부) 매장이고, 지역이 오면 그 지역
     * 매장만 남긴다. 좌표가 오면 가까운 순, 없으면 조회수 순으로 최대 {@code limit}곳이다.
     */
    public List<Long> recommendationCandidates(String ramenType, String region, BigDecimal latitude,
            BigDecimal longitude, int limit) {
        return shops.recommendationCandidates(ramenType, region, latitude, longitude, limit);
    }

    /** 순위대로 받은 매장 ID 중 추천해도 되는 매장을 최대 {@code limit}개 요약한다. 좌표가 오면 거리를 채운다. */
    public List<MobileShopSummary> recommendable(Long viewerId, List<Long> rankedShopIds, BigDecimal latitude,
            BigDecimal longitude, int limit) {
        if (rankedShopIds.isEmpty()) {
            return List.of();
        }
        return shops.recommendable(viewerId, rankedShopIds, latitude, longitude, limit);
    }

    /** 라멘 종류(앱 종류 값 중 하나), 없으면 태그 키워드로 추천해도 되는 매장 중 조회수 1위를 고른다. */
    public Optional<MobileShopSummary> mostPopular(Long viewerId, String ramenType, String tagKeyword) {
        return shops.mostPopular(viewerId, ramenType, tagKeyword);
    }

    /**
     * 추천 이유의 근거로 넘길 매장 소개다. 리뷰 AI 요약이 있으면 그것을, 없으면 매장 설명을 쓴다. 소개가 없는 매장은 빠진다.
     */
    @Transactional(readOnly = true)
    public Map<Long, String> introductions(Collection<Long> shopIds) {
        Map<Long, String> introductions = new HashMap<>();
        for (MobileShop shop : repository.findAllById(shopIds)) {
            String introduction = firstText(shop.getAiReviewSummary(), shop.getDescription());
            if (introduction != null) {
                introductions.put(shop.getId(), introduction);
            }
        }
        return introductions;
    }

    /** AI 요약이 아직 없는 공개 매장 ID를 {@code afterId} 다음부터 ID 순으로 돌려준다. */
    public List<Long> publishedShopIdsWithoutAiSummary(long afterId) {
        return repository.findPublishedIdsWithoutAiSummaryAfter(afterId);
    }

    /** 삭제되지 않은 매장의 AI 요약을 바꾼다. 그런 매장이 없으면 {@code false}다. */
    @Transactional
    public boolean saveAiSummary(Long shopId, String summary, List<String> keywords, Instant generatedAt) {
        return repository.updateAiSummary(shopId, summary, mapper.writeValueAsString(keywords), generatedAt) == 1;
    }

    private static String firstText(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return null;
    }

}
