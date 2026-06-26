package com.raota.application.ramenShop.search;

import com.raota.domain.ramenShop.model.RamenShop;
import org.springframework.stereotype.Component;

@Component
public class RamenShopComparisonSearchPolicy {

    private static final String DEFAULT_FOCUS = "기본 비교";
    private static final int DOCUMENT_LIMIT = 8;
    private static final double SIMILARITY_THRESHOLD = 0.3;

    public String normalizeFocus(String focus) {
        if (focus == null || focus.isBlank()) {
            return DEFAULT_FOCUS;
        }
        return focus.trim();
    }

    public String buildQuery(RamenShop shop, String focus) {
        if (!DEFAULT_FOCUS.equals(focus)) {
            return "%s %s".formatted(shop.getName(), focus);
        }

        return "%s 전반적인 맛 분위기 접근성 재방문 의사 메뉴 특징 리뷰".formatted(shop.getName());
    }

    public int documentLimit() {
        return DOCUMENT_LIMIT;
    }

    public double similarityThreshold() {
        return SIMILARITY_THRESHOLD;
    }
}
