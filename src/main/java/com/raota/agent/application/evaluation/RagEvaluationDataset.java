package com.raota.agent.application.evaluation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record RagEvaluationDataset(
        String version,
        List<RagEvaluationCase> cases
) {

    public RagEvaluationDataset {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("평가셋 버전은 필수입니다.");
        }
        cases = cases == null ? List.of() : List.copyOf(cases);
        validateCases(cases);
    }

    public List<RagEvaluationCase> casesFor(RagEvaluationSplit split) {
        return cases.stream().filter(item -> item.split() == split).toList();
    }

    public Map<RagEvaluationCaseType, Long> countsByType() {
        return cases.stream().collect(Collectors.groupingBy(
                RagEvaluationCase::type,
                () -> new EnumMap<>(RagEvaluationCaseType.class),
                Collectors.counting()
        ));
    }

    private static void validateCases(List<RagEvaluationCase> cases) {
        Set<String> ids = cases.stream().map(RagEvaluationCase::caseId).collect(Collectors.toSet());
        if (ids.size() != cases.size()) {
            throw new IllegalArgumentException("평가 사례 ID가 중복됩니다.");
        }
        cases.forEach(item -> {
            if (item.type() == RagEvaluationCaseType.SEARCH && item.relevantShops().isEmpty() && !item.expectsEmpty()) {
                throw new IllegalArgumentException("검색 사례는 정답 매장 또는 빈 결과 기대값이 필요합니다: " + item.caseId());
            }
        });
    }
}
