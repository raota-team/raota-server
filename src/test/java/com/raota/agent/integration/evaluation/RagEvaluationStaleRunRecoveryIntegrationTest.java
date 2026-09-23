package com.raota.agent.integration.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.agent.application.evaluation.RagEvaluationSplit;
import com.raota.agent.application.evaluation.RagEvaluationStaleRunRecoveryService;
import com.raota.agent.application.evaluation.RagEvaluationStatus;
import com.raota.agent.infrastructure.persistence.evaluation.RagEvaluationCaseResultJpaRepository;
import com.raota.agent.infrastructure.persistence.evaluation.RagEvaluationRunJpaRepository;
import com.raota.agent.infrastructure.persistence.evaluation.entity.RagEvaluationRunEntity;
import com.raota.agent.infrastructure.scheduler.RagEvaluationStaleRunScheduler;
import com.raota.support.BaseIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.support.TransactionTemplate;

class RagEvaluationStaleRunRecoveryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RagEvaluationStaleRunRecoveryService recoveryService;

    @Autowired
    private RagEvaluationRunJpaRepository runRepository;

    @Autowired
    private RagEvaluationCaseResultJpaRepository caseRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    @AfterEach
    void clean() {
        caseRepository.deleteAllInBatch();
        runRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("기본 임계값 10분을 넘긴 활성 실행을 복구하고 새 실행을 허용한다")
    void recoversRunPastDefaultThreshold() {
        String runId = saveRunning(LocalDateTime.now().minusMinutes(11));

        assertThat(recoveryService.recoverStaleRuns()).isEqualTo(1);
        assertThat(recoveryService.recoverStaleRuns()).isZero();

        RagEvaluationRunEntity run = runRepository.findById(runId).orElseThrow();
        assertThat(run.getStatus()).isEqualTo(RagEvaluationStatus.FAILED);
        assertThat(run.getFatalError()).isEqualTo(RagEvaluationStaleRunRecoveryService.STALE_HEARTBEAT_ERROR);
        runRepository.saveAndFlush(queued());
    }

    @Test
    @DisplayName("heartbeat가 임계값 안에 있으면 복구하지 않는다")
    void keepsRunWithinThreshold() {
        String runId = saveRunning(LocalDateTime.now().minusMinutes(9));

        assertThat(recoveryService.recoverStaleRuns()).isZero();
        assertThat(runRepository.findById(runId).orElseThrow().getStatus()).isEqualTo(RagEvaluationStatus.RUNNING);
    }

    @Test
    @DisplayName("테스트 프로필에서는 주기 복구 스케줄러를 등록하지 않는다")
    void schedulerDisabledInTestProfile() {
        assertThat(applicationContext.getBeanNamesForType(RagEvaluationStaleRunScheduler.class)).isEmpty();
    }

    private String saveRunning(LocalDateTime heartbeatAt) {
        String runId = runRepository.saveAndFlush(queued()).getRunId();
        transactionTemplate.execute(status -> runRepository.markRunning(runId, heartbeatAt));
        transactionTemplate.execute(status -> entityManager
            .createNativeQuery("UPDATE tb_rag_evaluation_run SET heartbeat_at = ?1 WHERE run_id = ?2")
            .setParameter(1, heartbeatAt)
            .setParameter(2, runId)
            .executeUpdate());
        return runId;
    }

    private static RagEvaluationRunEntity queued() {
        return RagEvaluationRunEntity.queued(UUID.randomUUID().toString(), "rag-mobile-v1.1", RagEvaluationSplit.DEV,
                UUID.randomUUID().toString(), LocalDateTime.now().plusHours(24), "test", "v1", "test", "{}");
    }

}
