package com.raota.application.ramenShop.search;

import com.raota.application.ramenShop.query.ParsedAiRamenShopSearchQuery;
import com.raota.application.ramenShop.result.AiRamenShopSearchHit;
import com.raota.application.ramenShop.result.RamenShopSearchDocument;
import com.raota.domain.retrieval.document.RetrievalMetadataKeys;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AiRamenShopSearchReranker {

    public List<AiRamenShopSearchHit> rerank(
            List<RamenShopSearchDocument> documents,
            ParsedAiRamenShopSearchQuery query,
            int limit
    ) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        Map<Long, AiRamenShopSearchHit> bestByShop = new LinkedHashMap<>();
        for (RamenShopSearchDocument document : documents) {
            Long shopId = parseShopId(document.metadata().get("shopId"));
            AiRamenShopSearchHit hit = new AiRamenShopSearchHit(
                    shopId,
                    document,
                    calculateFinalScore(document, query)
            );
            AiRamenShopSearchHit current = bestByShop.get(shopId);
            if (current == null || hit.finalScore() > current.finalScore()) {
                bestByShop.put(shopId, hit);
            }
        }

        return bestByShop.values().stream()
                .sorted(Comparator.comparingDouble(AiRamenShopSearchHit::finalScore).reversed())
                .limit(limit)
                .toList();
    }

    private double calculateFinalScore(RamenShopSearchDocument document, ParsedAiRamenShopSearchQuery query) {
        double score = document.score();
        List<String> menuNames = metadataValues(document.metadata().get(RetrievalMetadataKeys.MENU_NAMES));
        List<String> tags = metadataValues(document.metadata().get(RetrievalMetadataKeys.TAGS));
        String region = String.valueOf(document.metadata().getOrDefault(RetrievalMetadataKeys.REGION, ""));
        String text = document.text() == null ? "" : document.text().toLowerCase(Locale.ROOT);

        for (String keyword : query.foodKeywords()) {
            String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
            if (containsAny(menuNames, normalizedKeyword)) {
                score += 0.20;
            }
            if (containsAny(tags, normalizedKeyword)) {
                score += 0.10;
            }
            if (text.contains(normalizedKeyword)) {
                score += 0.05;
            }
        }

        for (String queryRegion : query.regions()) {
            if (region.contains(queryRegion)) {
                score += 0.10;
            }
        }

        return score;
    }

    private boolean containsAny(List<String> values, String keyword) {
        return values.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains(keyword));
    }

    private List<String> metadataValues(Object value) {
        if (value instanceof List<?> values) {
            return values.stream()
                    .map(String::valueOf)
                    .toList();
        }
        if (value == null) {
            return List.of();
        }
        return new ArrayList<>(List.of(String.valueOf(value)));
    }

    private Long parseShopId(Object rawShopId) {
        if (rawShopId == null) {
            throw new IllegalArgumentException("추천 결과에 shopId 메타데이터가 없습니다.");
        }

        String value = rawShopId.toString().trim().replace("\"", "");
        return Long.valueOf(value);
    }
}
