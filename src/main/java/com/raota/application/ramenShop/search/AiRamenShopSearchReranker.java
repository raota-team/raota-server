package com.raota.application.ramenShop.search;

import com.raota.application.ramenShop.query.ParsedAiRamenShopSearchQuery;
import com.raota.application.ramenShop.result.AiRamenShopSearchHit;
import com.raota.application.ramenShop.result.RamenShopSearchDocument;
import com.raota.domain.retrieval.document.RetrievalDocumentType;
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

        Map<Long, ShopSearchEvidence> evidenceByShop = new LinkedHashMap<>();
        for (RamenShopSearchDocument document : documents) {
            Long shopId = parseShopId(document.metadata().get(RetrievalMetadataKeys.SHOP_ID));
            if (shopId == null) {
                continue;
            }
            evidenceByShop.computeIfAbsent(shopId, ShopSearchEvidence::new)
                    .add(document, calculateBoost(document, query));
        }

        return evidenceByShop.values().stream()
                .map(ShopSearchEvidence::toHit)
                .sorted(Comparator.comparingDouble(AiRamenShopSearchHit::finalScore).reversed())
                .limit(limit)
                .toList();
    }

    private double calculateBoost(RamenShopSearchDocument document, ParsedAiRamenShopSearchQuery query) {
        double boost = 0;
        List<String> menuNames = metadataValues(document.metadata().get(RetrievalMetadataKeys.MENU_NAMES));
        List<String> tags = metadataValues(document.metadata().get(RetrievalMetadataKeys.TAGS));
        String region = String.valueOf(document.metadata().getOrDefault(RetrievalMetadataKeys.REGION, ""));
        String text = document.text() == null ? "" : document.text().toLowerCase(Locale.ROOT);

        for (String keyword : query.foodKeywords()) {
            String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
            if (containsAny(menuNames, normalizedKeyword)) {
                boost += 0.20;
            }
            if (containsAny(tags, normalizedKeyword)) {
                boost += 0.10;
            }
            if (text.contains(normalizedKeyword)) {
                boost += 0.05;
            }
        }

        for (String queryRegion : query.regions()) {
            if (region.contains(queryRegion)) {
                boost += 0.10;
            }
        }

        boost += calculateFoodTypeBoost(menuNames, tags, text, query.foodTypes());

        return boost;
    }

    private double calculateFoodTypeBoost(List<String> menuNames, List<String> tags, String text, List<String> foodTypes) {
        if (foodTypes == null || foodTypes.isEmpty()) {
            return 0;
        }

        List<String> requestedAliases = RamenFoodKeywordDictionary.aliasesFor(foodTypes);
        List<String> conflictAliases = RamenFoodKeywordDictionary.conflictAliasesFor(foodTypes);
        boolean requestedStructuredMatch = containsAny(menuNames, requestedAliases) || containsAny(tags, requestedAliases);
        boolean requestedTextMatch = containsAny(text, requestedAliases);
        boolean conflictStructuredMatch = containsAny(menuNames, conflictAliases) || containsAny(tags, conflictAliases);
        boolean conflictTextMatch = containsAny(text, conflictAliases);

        double boost = 0;
        if (requestedStructuredMatch) {
            boost += 0.45;
        }
        if (requestedTextMatch) {
            boost += 0.12;
        }
        if (!requestedStructuredMatch && !requestedTextMatch) {
            if (conflictStructuredMatch) {
                boost -= 0.35;
            }
            if (conflictTextMatch) {
                boost -= 0.15;
            }
        }

        return boost;
    }

    private boolean containsAny(List<String> values, String keyword) {
        return values.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains(keyword));
    }

    private boolean containsAny(List<String> values, List<String> keywords) {
        return values.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> containsAny(value, keywords));
    }

    private boolean containsAny(String value, List<String> keywords) {
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(value::contains);
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
            return null;
        }

        String value = rawShopId.toString().trim().replace("\"", "");
        if (value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }

    private boolean isProfileDocument(RamenShopSearchDocument document) {
        return RetrievalDocumentType.SHOP_PROFILE.name()
                .equals(String.valueOf(document.metadata().get(RetrievalMetadataKeys.DOCUMENT_TYPE)));
    }

    private class ShopSearchEvidence {
        private final Long shopId;
        private double bestProfileScore;
        private double bestReviewScore;
        private double bestBoost;
        private int reviewHitCount;

        private ShopSearchEvidence(Long shopId) {
            this.shopId = shopId;
        }

        private void add(RamenShopSearchDocument document, double boost) {
            if (isProfileDocument(document)) {
                bestProfileScore = Math.max(bestProfileScore, document.score());
            } else {
                bestReviewScore = Math.max(bestReviewScore, document.score());
                reviewHitCount++;
            }
            bestBoost = Math.max(bestBoost, boost);
        }

        private AiRamenShopSearchHit toHit() {
            double finalScore = bestProfileScore * 0.45
                    + bestReviewScore * 0.35
                    + Math.log(reviewHitCount + 1) * 0.08
                    + bestBoost;

            return new AiRamenShopSearchHit(shopId, finalScore);
        }
    }
}
