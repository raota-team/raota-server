package com.raota.mobile.shop.application.facade;

import com.raota.mobile.shop.application.result.MobileShopSummary;
import com.raota.mobile.shop.application.service.MobileShopQueryService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** AI 추천(agent)이 고른 매장을 v2 매장 기준으로 다시 확인해 앱 목록 형태로 돌려준다. */
@Service
@RequiredArgsConstructor
public class MobileShopAgentFacade {

    private final MobileShopQueryService shops;

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

}
