package com.raota.agent.application.evaluation;

import com.raota.agent.infrastructure.persistence.evaluation.RagEvaluationRunJpaRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * heartbeat가 멈춘 QUEUED/RUNNING 실행을 FAILED로 정리해 활성 슬롯을 반환한다.
 *
 * <p>조건부 단일 UPDATE라 여러 서버에서 동시에 실행하거나 반복 실행해도 결과가 같다.</p>
 */
@Service
public class RagEvaluationStaleRunRecoveryService {

    public static final String STALE_HEARTBEAT_ERROR = "STALE_HEARTBEAT";
    private static final int MIN_THRESHOLD_TO_HEARTBEAT_RATIO = 3;

    private final RagEvaluationRunJpaRepository runRepository;
    private final Duration staleThreshold;

    public RagEvaluationStaleRunRecoveryService(
            RagEvaluationRunJpaRepository runRepository,
            @Value("${app.rag.evaluation.stale-threshold:PT10M}") Duration staleThreshold,
            @Value("${app.rag.evaluation.heartbeat-interval:PT30S}") Duration heartbeatInterval
    ) {
        if (staleThreshold == null || heartbeatInterval == null
                || staleThreshold.compareTo(heartbeatInterval.multipliedBy(MIN_THRESHOLD_TO_HEARTBEAT_RATIO)) < 0) {
            throw new IllegalArgumentException(
                    "app.rag.evaluation.stale-threshold는 heartbeat-interval의 3배 이상이어야 합니다.");
        }
        this.runRepository = runRepository;
        this.staleThreshold = staleThreshold;
    }

    @Transactional
    public int recoverStaleRuns() {
        LocalDateTime now = LocalDateTime.now();
        return runRepository.failStale(now.minus(staleThreshold), STALE_HEARTBEAT_ERROR, now);
    }
}
