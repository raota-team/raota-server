package com.raota.agent.application.evaluation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;
import java.util.List;

public record RagEvaluationCase(String caseId, RagEvaluationCaseType type, RagEvaluationSplit split, JsonNode request,
        String authMode, List<RagExpectedShop> relevantShops, Boolean expectsEmpty, List<String> requiredFacts,
        List<String> allowedEvidence, List<String> forbiddenClaims, Boolean expectsFallback, Boolean contractOnly,
        Integer primaryK, Integer diagnosticK, RagExpectedError expectedError) {

    /** Backward-compatible constructor for callers that predate the evidence label. */
    public RagEvaluationCase(String caseId, RagEvaluationCaseType type, RagEvaluationSplit split, JsonNode request,
            String authMode, List<RagExpectedShop> relevantShops, Boolean expectsEmpty, List<String> requiredFacts,
            List<String> forbiddenClaims, Boolean expectsFallback, Boolean contractOnly, Integer primaryK,
            Integer diagnosticK) {
        this(caseId, type, split, request, authMode, relevantShops, expectsEmpty, requiredFacts, List.of(),
                forbiddenClaims, expectsFallback, contractOnly, primaryK, diagnosticK, null);
    }

    /** Backward-compatible constructor for callers using the full v1.1 shape. */
    public RagEvaluationCase(String caseId, RagEvaluationCaseType type, RagEvaluationSplit split, JsonNode request,
            String authMode, List<RagExpectedShop> relevantShops, Boolean expectsEmpty, List<String> requiredFacts,
            List<String> allowedEvidence, List<String> forbiddenClaims, Boolean expectsFallback, Boolean contractOnly,
            Integer primaryK, Integer diagnosticK) {
        this(caseId, type, split, request, authMode, relevantShops, expectsEmpty, requiredFacts, allowedEvidence,
                forbiddenClaims, expectsFallback, contractOnly, primaryK, diagnosticK, null);
    }

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
        allowedEvidence = allowedEvidence == null ? List.of() : List.copyOf(allowedEvidence);
        forbiddenClaims = forbiddenClaims == null ? List.of() : List.copyOf(forbiddenClaims);
        expectsEmpty = Boolean.TRUE.equals(expectsEmpty);
        expectsFallback = Boolean.TRUE.equals(expectsFallback);
        contractOnly = Boolean.TRUE.equals(contractOnly);
        primaryK = primaryK == null || primaryK < 1 ? 1 : primaryK;
        diagnosticK = diagnosticK == null || diagnosticK < primaryK ? Math.max(6, primaryK) : diagnosticK;
        if (expectedError != null && expectedError.code() == null) {
            throw new IllegalArgumentException("예상 오류 코드는 필수입니다.");
        }
        if (expectedError != null) {
            expectsFallback = false;
        }
    }
}
