package com.raota.agent.infrastructure.scheduler;

import com.raota.agent.application.evaluation.RagEvaluationStaleRunRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rag.evaluation.stale-recovery.enabled", havingValue = "true", matchIfMissing = true)
public class RagEvaluationStaleRunScheduler {

    private final RagEvaluationStaleRunRecoveryService recoveryService;

    @Scheduled(initialDelayString = "${app.rag.evaluation.stale-check-interval:PT1M}",
            fixedDelayString = "${app.rag.evaluation.stale-check-interval:PT1M}")
    public void recoverStaleRuns() {
        try {
            int recovered = recoveryService.recoverStaleRuns();
            if (recovered > 0) {
                log.warn("Marked {} stale RAG evaluation runs as FAILED", recovered);
            }
        }
        catch (Exception exception) {
            log.error("Failed to recover stale RAG evaluation runs", exception);
        }
    }

}
