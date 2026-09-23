package com.raota.agent.application.evaluation;

import tools.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

public final class RagEvaluationMetricCalculator {

    private RagEvaluationMetricCalculator() {
    }

    public static Map<String, Double> calculateSearch(List<Long> returnedShopIds, List<RagExpectedShop> expectedShops,
            boolean expectsEmpty, int primaryK, int diagnosticK) {
        List<Long> returned = returnedShopIds == null ? List.of() : List.copyOf(returnedShopIds);
        List<RagExpectedShop> expected = expectedShops == null ? List.of() : List.copyOf(expectedShops);
        Map<String, Double> metrics = new LinkedHashMap<>();

        if (expectsEmpty || expected.isEmpty()) {
            metrics.put("emptyResultAccuracy", returned.isEmpty() ? 1.0 : 0.0);
            return metrics;
        }

        int primary = Math.max(1, primaryK);
        int diagnostic = Math.max(primary, diagnosticK);
        metrics.put("hitrateAt" + primary, hitRate(returned, expected, primary));
        metrics.put("recallAt" + primary, recall(returned, expected, primary));
        metrics.put("ndcgAt" + primary, ndcg(returned, expected, primary));
        metrics.put("hitrateAt" + diagnostic, hitRate(returned, expected, diagnostic));
        metrics.put("recallAt" + diagnostic, recall(returned, expected, diagnostic));
        metrics.put("precisionAt" + diagnostic, precision(returned, expected, diagnostic));
        metrics.put("mrrAt" + diagnostic, reciprocalRank(returned, expected, diagnostic));
        metrics.put("ndcgAt" + diagnostic, ndcg(returned, expected, diagnostic));
        return metrics;
    }

    public static Map<String, Double> average(List<Map<String, Double>> caseMetrics) {
        if (caseMetrics == null || caseMetrics.isEmpty()) {
            return Map.of();
        }

        Map<String, List<Double>> valuesByMetric = new LinkedHashMap<>();
        caseMetrics.stream()
            .filter(Objects::nonNull)
            .flatMap(metrics -> metrics.entrySet().stream())
            .forEach(entry -> valuesByMetric.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>())
                .add(entry.getValue()));

        return valuesByMetric.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> average(entry.getValue(), value -> value),
                    (left, right) -> left, LinkedHashMap::new));
    }

    public static Map<String, Double> calculateGeneration(RagEvaluationCase evaluationCase, JsonNode response,
            boolean fallback) {
        String content = response == null || response.isNull() ? "" : response.toString().toLowerCase(Locale.ROOT);
        long requiredCount = evaluationCase.requiredFacts().size();
        long coveredCount = evaluationCase.requiredFacts()
            .stream()
            .filter(fact -> content.contains(fact.toLowerCase(Locale.ROOT)))
            .count();
        long forbiddenCount = evaluationCase.forbiddenClaims()
            .stream()
            .filter(claim -> content.contains(claim.toLowerCase(Locale.ROOT)))
            .count();

        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("schemaValid", isSchemaValid(evaluationCase.type(), response) ? 1.0 : 0.0);
        metrics.put("requiredFactCoverage", requiredCount == 0 ? 1.0 : (double) coveredCount / requiredCount);
        metrics.put("forbiddenClaimRate", evaluationCase.forbiddenClaims().isEmpty() ? 0.0
                : (double) forbiddenCount / evaluationCase.forbiddenClaims().size());
        metrics.put("fallbackAccuracy", fallback == evaluationCase.expectsFallback() ? 1.0 : 0.0);
        return metrics;
    }

    public static Map<String, Double> calculateExpectedError(boolean matched) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("expectedErrorMatch", matched ? 1.0 : 0.0);
        return metrics;
    }

    private static boolean isSchemaValid(RagEvaluationCaseType type, JsonNode response) {
        if (response == null || response.isNull() || !response.isObject() || type == null) {
            return false;
        }

        return switch (type) {
            case SUMMARY -> isSummarySchemaValid(response);
            case CHAT -> isChatSchemaValid(response);
            case COMPARE -> isCompareSchemaValid(response);
            case SEARCH -> false;
        };
    }

    private static boolean isSummarySchemaValid(JsonNode response) {
        JsonNode shopInfo = response.get("shopInfo");
        JsonNode summary = response.get("summary");
        JsonNode reviewCount = response.get("reviewCount");
        JsonNode sampleReviews = response.get("sampleReviews");
        return hasObjectFields(shopInfo, "id", "name", "type", "location", "imageUrl", "isBookmarked")
                && reviewCount != null && reviewCount.isNumber()
                && hasObjectFields(summary, "pros", "cons", "recommendedMenu") && hasSummaryDetail(summary.get("pros"))
                && hasSummaryDetail(summary.get("cons")) && hasSummaryDetail(summary.get("recommendedMenu"))
                && sampleReviews != null && sampleReviews.isArray();
    }

    private static boolean hasSummaryDetail(JsonNode detail) {
        return hasObjectFields(detail, "title", "body") && hasTextField(detail, "title")
                && hasTextField(detail, "body");
    }

    private static boolean isChatSchemaValid(JsonNode response) {
        JsonNode message = response.get("message");
        return hasObjectFields(message, "role", "content") && hasTextField(message, "role")
                && hasTextField(message, "content");
    }

    private static boolean isCompareSchemaValid(JsonNode response) {
        JsonNode shopA = response.get("shopA");
        JsonNode shopB = response.get("shopB");
        JsonNode narratives = response.get("narratives");
        if (!hasObjectFields(shopA, "id", "name") || !hasTextField(shopA, "name")
                || !hasObjectFields(shopB, "id", "name") || !hasTextField(shopB, "name") || narratives == null
                || !narratives.isArray()) {
            return false;
        }
        for (JsonNode narrative : narratives) {
            if (!hasObjectFields(narrative, "title", "body") || !hasTextField(narrative, "title")
                    || !hasTextField(narrative, "body")) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasObjectFields(JsonNode node, String... fields) {
        if (node == null || !node.isObject()) {
            return false;
        }
        for (String field : fields) {
            if (!node.has(field) || node.get(field) == null || node.get(field).isNull()) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasTextField(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && value.isString() && !value.asText().isBlank();
    }

    private static double hitRate(List<Long> returned, List<RagExpectedShop> expected, int k) {
        return returned.stream()
            .limit(Math.max(0, k))
            .anyMatch(shopId -> expected.stream().anyMatch(item -> item.shopId().equals(shopId))) ? 1.0 : 0.0;
    }

    private static double recall(List<Long> returned, List<RagExpectedShop> expected, int k) {
        long matched = expected.stream()
            .filter(item -> returned.stream().limit(Math.max(0, k)).anyMatch(item.shopId()::equals))
            .count();
        return (double) matched / expected.size();
    }

    private static double precision(List<Long> returned, List<RagExpectedShop> expected, int k) {
        int denominator = Math.max(1, k);
        long matched = returned.stream()
            .limit(Math.max(0, k))
            .filter(shopId -> expected.stream().anyMatch(item -> item.shopId().equals(shopId)))
            .count();
        return (double) matched / denominator;
    }

    private static double reciprocalRank(List<Long> returned, List<RagExpectedShop> expected, int k) {
        for (int index = 0; index < Math.min(Math.max(0, k), returned.size()); index++) {
            Long shopId = returned.get(index);
            if (expected.stream().anyMatch(item -> item.shopId().equals(shopId))) {
                return 1.0 / (index + 1);
            }
        }
        return 0.0;
    }

    private static double ndcg(List<Long> returned, List<RagExpectedShop> expected, int k) {
        Map<Long, Integer> relevanceByShop = expected.stream()
            .collect(Collectors.toMap(RagExpectedShop::shopId, RagExpectedShop::relevance, Math::max));
        double dcg = 0.0;
        for (int index = 0; index < Math.min(Math.max(0, k), returned.size()); index++) {
            int relevance = relevanceByShop.getOrDefault(returned.get(index), 0);
            dcg += (Math.pow(2, relevance) - 1) / log2(index + 2);
        }

        List<Integer> ideal = expected.stream()
            .map(RagExpectedShop::relevance)
            .sorted((left, right) -> Integer.compare(right, left))
            .limit(Math.max(0, k))
            .toList();
        double idealDcg = 0.0;
        for (int index = 0; index < ideal.size(); index++) {
            idealDcg += (Math.pow(2, ideal.get(index)) - 1) / log2(index + 2);
        }
        return idealDcg == 0.0 ? 0.0 : dcg / idealDcg;
    }

    private static double log2(int value) {
        return Math.log(value) / Math.log(2);
    }

    private static double average(List<Double> values, ToDoubleFunction<Double> mapper) {
        return values.stream().mapToDouble(mapper).average().orElse(0.0);
    }

}
