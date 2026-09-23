package com.raota.agent.application.evaluation;

import java.util.List;

public record RagEvaluationJudgeResult(int groundedness, List<String> unsupportedClaims,
        boolean informationSufficiencyCorrect, String verdict, String reason, String judgeVersion) {
    public RagEvaluationJudgeResult {
        groundedness = Math.max(0, Math.min(2, groundedness));
        unsupportedClaims = unsupportedClaims == null ? List.of() : List.copyOf(unsupportedClaims);
        verdict = verdict == null ? "REVIEW" : verdict;
        reason = reason == null ? "" : reason;
        judgeVersion = judgeVersion == null ? "rule-v1" : judgeVersion;
    }
}
