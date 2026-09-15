package com.raota.agent.application.evaluation;

public interface RagEvaluationJudge {
    RagEvaluationJudgeResult suggest(RagEvaluationCase evaluationCase, RagExecutionResult executionResult);
}
