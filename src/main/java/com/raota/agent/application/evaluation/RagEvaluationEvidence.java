package com.raota.agent.application.evaluation;

import java.util.Map;

/** A compact, reviewable representation of the documents used by an evaluation case. */
public record RagEvaluationEvidence(
        String sourceId,
        String documentType,
        String source,
        String text,
        Map<String, Object> metadata
) {
    public RagEvaluationEvidence {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        text = text == null ? "" : text;
    }
}
