package com.raota.agent.application.evaluation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;
import java.util.List;

/** Result shared by the four RAG evaluation executors and the persistence layer. */
public record RagExecutionResult(
        RagEvaluationCaseStatus status,
        JsonNode response,
        List<Long> returnedShopIds,
        List<RagEvaluationEvidence> evidence,
        boolean fallback,
        long latencyMs,
        String errorType,
        String errorMessage
) {
    public RagExecutionResult {
        status = status == null ? RagEvaluationCaseStatus.ERROR : status;
        response = response == null ? NullNode.getInstance() : response;
        returnedShopIds = returnedShopIds == null ? List.of() : List.copyOf(returnedShopIds);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        errorType = errorType == null ? "" : errorType;
        errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public static RagExecutionResult skipped() {
        return new RagExecutionResult(
                RagEvaluationCaseStatus.SKIPPED,
                NullNode.getInstance(),
                List.of(),
                List.of(),
                false,
                0,
                "",
                "contract-only 사례는 서버 계약 실행에서 제외했습니다."
        );
    }

    public static RagExecutionResult error(long latencyMs, Throwable throwable) {
        return new RagExecutionResult(
                RagEvaluationCaseStatus.ERROR,
                NullNode.getInstance(),
                List.of(),
                List.of(),
                false,
                latencyMs,
                throwable == null ? "UNKNOWN" : throwable.getClass().getSimpleName(),
                throwable == null || throwable.getMessage() == null ? "평가 사례 실행에 실패했습니다." : throwable.getMessage()
        );
    }
}
