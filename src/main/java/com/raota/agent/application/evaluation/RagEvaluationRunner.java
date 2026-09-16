package com.raota.agent.application.evaluation;

/**
 * Application boundary for executing one frozen evaluation split.
 *
 * <p>The runner is asynchronous in the default implementation, while callers
 * only depend on the execution contract and do not need to know its scheduling
 * or persistence details.</p>
 */
public interface RagEvaluationRunner {
    void execute(String runId, RagEvaluationDataset dataset, RagEvaluationSplit split);
}
