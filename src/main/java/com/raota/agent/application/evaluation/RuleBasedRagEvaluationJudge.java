package com.raota.agent.application.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Deterministic baseline judge. It keeps the first evaluation repeatable while an
 * optional LLM judge can be introduced behind the same port later.
 */
@Service
public class RuleBasedRagEvaluationJudge implements RagEvaluationJudge {

    @Override
    public RagEvaluationJudgeResult suggest(RagEvaluationCase evaluationCase, RagExecutionResult executionResult) {
        String response = executionResult == null || executionResult.response() == null ? ""
                : executionResult.response().toString().toLowerCase(Locale.ROOT);
        List<String> unsupportedClaims = new ArrayList<>();
        evaluationCase.forbiddenClaims()
            .stream()
            .filter(claim -> response.contains(claim.toLowerCase(Locale.ROOT)))
            .forEach(unsupportedClaims::add);

        int groundedness;
        if (executionResult == null || executionResult.status() == RagEvaluationCaseStatus.ERROR) {
            groundedness = 0;
        }
        else if (executionResult.fallback() && evaluationCase.expectsFallback()) {
            groundedness = 2;
        }
        else if (unsupportedClaims.isEmpty()) {
            groundedness = response.isBlank() ? 0 : 1;
        }
        else {
            groundedness = 0;
        }

        boolean sufficiencyCorrect = executionResult != null
                && executionResult.fallback() == evaluationCase.expectsFallback();
        String verdict = groundedness >= 1 && unsupportedClaims.isEmpty() && sufficiencyCorrect ? "PASS" : "REVIEW";
        String reason = unsupportedClaims.isEmpty() ? "필수 사실·금지 주장 규칙을 결정적으로 검사한 제안입니다."
                : "금지된 주장 후보가 발견되어 관리자의 검수가 필요합니다.";

        return new RagEvaluationJudgeResult(groundedness, unsupportedClaims, sufficiencyCorrect, verdict, reason,
                "rule-v1");
    }

}
