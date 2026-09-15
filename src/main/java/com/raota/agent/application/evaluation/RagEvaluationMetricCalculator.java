package com.raota.agent.application.evaluation;

import tools.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

public final class RagEvaluationMetricCalculator {

    private RagEvaluationMetricCalculator() {
    }

    public static Map<String, Double> calculateSearch(
            List<Long> returnedShopIds,
            List<RagExpectedShop> expectedShops,
            boolean expectsEmpty,
            int primaryK,
            int diagnosticK
    ) {
        List<Long> returned = returnedShopIds == null ? List.of() : List.copyOf(returnedShopIds);
        List<RagExpectedShop> expected = expectedShops == null ? List.of() : List.copyOf(expectedShops);
        Map<String, Double> metrics = new LinkedHashMap<>();

        if (expectsEmpty || expected.isEmpty()) {
            metrics.put("emptyResultAccuracy", returned.isEmpty() ? 1.0 : 0.0);
            return metrics;
        }

        metrics.put("hitrateAt1", hitRate(returned, expected, 1));
        metrics.put("recallAt1", recall(returned, expected, 1));
        metrics.put("ndcgAt1", ndcg(returned, expected, 1));
        metrics.put("hitrateAt" + diagnosticK, hitRate(returned, expected, diagnosticK));
        metrics.put("recallAt" + diagnosticK, recall(returned, expected, diagnosticK));
        metrics.put("precisionAt" + diagnosticK, precision(returned, expected, diagnosticK));
        metrics.put("mrrAt" + diagnosticK, reciprocalRank(returned, expected, diagnosticK));
        metrics.put("ndcgAt" + diagnosticK, ndcg(returned, expected, diagnosticK));
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
                .forEach(entry -> valuesByMetric
                        .computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>())
                        .add(entry.getValue()));

        return valuesByMetric.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> average(entry.getValue(), value -> value),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    public static Map<String, Double> calculateGeneration(
            RagEvaluationCase evaluationCase,
            JsonNode response,
            boolean fallback
    ) {
        String content = response == null || response.isNull() ? "" : response.toString().toLowerCase();
        long requiredCount = evaluationCase.requiredFacts().size();
        long coveredCount = evaluationCase.requiredFacts().stream()
                .filter(fact -> content.contains(fact.toLowerCase()))
                .count();
        long forbiddenCount = evaluationCase.forbiddenClaims().stream()
                .filter(claim -> content.contains(claim.toLowerCase()))
                .count();

        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("schemaValid", response == null || response.isNull() ? 0.0 : 1.0);
        metrics.put("requiredFactCoverage", requiredCount == 0 ? 1.0 : (double) coveredCount / requiredCount);
        metrics.put("forbiddenClaimRate", evaluationCase.forbiddenClaims().isEmpty()
                ? 0.0 : (double) forbiddenCount / evaluationCase.forbiddenClaims().size());
        metrics.put("fallbackAccuracy", fallback == evaluationCase.expectsFallback() ? 1.0 : 0.0);
        return metrics;
    }

    private static double hitRate(List<Long> returned, List<RagExpectedShop> expected, int k) {
        return returned.stream()
                .limit(Math.max(0, k))
                .anyMatch(shopId -> expected.stream().anyMatch(item -> item.shopId().equals(shopId)))
                ? 1.0 : 0.0;
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
                .collect(Collectors.toMap(
                        RagExpectedShop::shopId,
                        RagExpectedShop::relevance,
                        Math::max
                ));
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
