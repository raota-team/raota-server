package com.raota.agent.application.evaluation;

import java.util.Locale;

/**
 * Describes an error that is part of an evaluation case's contract.
 *
 * <p>
 * An expected error is different from a fallback response. A fallback is a successful
 * response with limited information, while an expected error is a deliberately invalid
 * request or missing resource that must be rejected by the application.
 * </p>
 */
public record RagExpectedError(String code, Integer httpStatus, String messageContains) {

    public RagExpectedError {
        code = code == null || code.isBlank() ? null : code.trim().toUpperCase(Locale.ROOT);
        if (httpStatus != null && (httpStatus < 400 || httpStatus > 599)) {
            throw new IllegalArgumentException("예상 오류 HTTP 상태는 4xx 또는 5xx여야 합니다.");
        }
        messageContains = messageContains == null || messageContains.isBlank() ? null : messageContains.trim();
    }

    public boolean matches(RagExecutionResult result) {
        if (result == null || result.status() != RagEvaluationCaseStatus.ERROR) {
            return false;
        }
        if (messageContains != null
                && (result.errorMessage() == null || !result.errorMessage().contains(messageContains))) {
            return false;
        }
        if (code == null) {
            return false;
        }
        return switch (code) {
            case "RESOURCE_NOT_FOUND" -> "EntityNotFoundException".equals(result.errorType());
            case "VALIDATION_ERROR" -> "IllegalArgumentException".equals(result.errorType());
            default -> true;
        };
    }
}
