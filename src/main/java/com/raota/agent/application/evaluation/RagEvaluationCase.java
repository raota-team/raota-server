package com.raota.agent.application.evaluation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;
import java.util.List;

public record RagEvaluationCase(
        String caseId,
        RagEvaluationCaseType type,
        RagEvaluationSplit split,
        JsonNode request,
        String authMode,
        List<RagExpectedShop> relevantShops,
        boolean expectsEmpty,
        List<String> requiredFacts,
        List<String> forbiddenClaims,
        boolean expectsFallback,
        boolean contractOnly,
        Integer primaryK,
        Integer diagnosticK
) {

    public RagEvaluationCase {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("평가 사례 ID는 필수입니다.");
        }
        if (type == null || split == null) {
            throw new IllegalArgumentException("평가 사례 유형과 split은 필수입니다.");
        }
        request = request == null ? NullNode.getInstance() : request;
        authMode = authMode == null || authMode.isBlank() ? "ANONYMOUS" : authMode;
        relevantShops = relevantShops == null ? List.of() : List.copyOf(relevantShops);
        requiredFacts = requiredFacts == null ? List.of() : List.copyOf(requiredFacts);
        forbiddenClaims = forbiddenClaims == null ? List.of() : List.copyOf(forbiddenClaims);
        primaryK = primaryK == null || primaryK < 1 ? 1 : primaryK;
        diagnosticK = diagnosticK == null || diagnosticK < primaryK ? Math.max(6, primaryK) : diagnosticK;
    }
}
