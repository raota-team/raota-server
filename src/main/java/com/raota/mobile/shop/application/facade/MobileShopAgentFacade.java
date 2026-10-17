package com.raota.mobile.shop.application.facade;

import com.raota.mobile.shop.application.result.MobileShopSummary;
import com.raota.mobile.shop.application.service.MobileShopQueryService;
import com.raota.mobile.shop.domain.repository.MobileShopRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
     * 추천 순위대로 받은 매장 ID 중 추천해도 되는 매장을 최대 {@code limit}개 고른다. 지역({@code region})이나 좌표·반경이
     * 오면 그 안의 매장만 남긴다. 좌표는 위도·경도가 함께 오거나 함께 비어 있어야 한다.
     */
    public List<MobileShopSummary> recommendable(Long viewerId, List<Long> rankedShopIds, String region,
            BigDecimal latitude, BigDecimal longitude, int radiusMeters, int limit) {
        if (rankedShopIds.isEmpty()) {
            return List.of();
        }
        return shops.recommendable(viewerId, rankedShopIds, region, latitude, longitude, radiusMeters, limit);
    }

    /** 라멘 종류(앱 값 10개 중 하나), 없으면 태그 키워드로 추천해도 되는 매장 중 조회수 1위를 고른다. */
    public Optional<MobileShopSummary> mostPopular(Long viewerId, String ramenType, String tagKeyword) {
        return shops.mostPopular(viewerId, ramenType, tagKeyword);
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

}
