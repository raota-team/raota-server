package com.raota.agent.integration.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.raota.agent.application.evaluation.RagEvaluationSplit;
import com.raota.agent.application.evaluation.RagEvaluationStatus;
import com.raota.agent.infrastructure.persistence.evaluation.RagEvaluationCaseResultJpaRepository;
import com.raota.agent.infrastructure.persistence.evaluation.RagEvaluationRunJpaRepository;
import com.raota.agent.infrastructure.persistence.evaluation.entity.RagEvaluationRunEntity;
import com.raota.support.BaseIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.IntSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

class RagEvaluationRunStateTransitionIntegrationTest extends BaseIntegrationTest {

    private static final String STALE = "STALE_HEARTBEAT";

    @Autowired
    private RagEvaluationRunJpaRepository runRepository;

    @Autowired
    private RagEvaluationCaseResultJpaRepository caseRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    @AfterEach
    void clean() {
        caseRepository.deleteAllInBatch();
        runRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("활성 실행은 active_slot 유니크 제약으로 한 건만 저장된다")
    void onlyOneActiveRunCanExist() {
        runRepository.saveAndFlush(queued());

        assertThatThrownBy(() -> runRepository.saveAndFlush(queued()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(runRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("종료된 실행은 활성 슬롯을 비워 새 실행을 허용한다")
    void finishedRunReleasesActiveSlot() {
        String runId = runRepository.saveAndFlush(queued()).getRunId();
        LocalDateTime now = LocalDateTime.now();

        assertThat(inTx(() -> runRepository.markRunning(runId, now))).isEqualTo(1);
        assertThat(inTx(() -> runRepository.markReviewRequired(runId, "{}", now))).isEqualTo(1);

        runRepository.saveAndFlush(queued());
        assertThat(runRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("상태 전이는 기대 상태일 때만 적용된다")
    void transitionsRequireExpectedStatus() {
        String runId = runRepository.saveAndFlush(queued()).getRunId();
        LocalDateTime now = LocalDateTime.now();

        assertThat(inTx(() -> runRepository.touchHeartbeat(runId, now))).isZero();
        assertThat(inTx(() -> runRepository.markReviewRequired(runId, "{}", now))).isZero();
        assertThat(inTx(() -> runRepository.markRunning(runId, now))).isEqualTo(1);
        assertThat(inTx(() -> runRepository.markRunning(runId, now))).isZero();
        assertThat(inTx(() -> runRepository.touchHeartbeat(runId, now))).isEqualTo(1);
    }

    @Test
    @DisplayName("heartbeat가 멈춘 실행만 FAILED로 복구하고 반복 실행해도 결과가 같다")
    void failStaleIsConditionalAndIdempotent() {
        String reviewedId = runRepository.saveAndFlush(queued()).getRunId();
        LocalDateTime now = LocalDateTime.now();
        inTx(() -> runRepository.markRunning(reviewedId, now));
        inTx(() -> runRepository.markReviewRequired(reviewedId, "{}", now));
        backdateHeartbeat(reviewedId, now.minusHours(1));
        String staleId = runningRunWithHeartbeat(LocalDateTime.now().minusMinutes(11));

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
        assertThat(inTx(() -> runRepository.failStale(threshold, STALE, LocalDateTime.now()))).isEqualTo(1);
        assertThat(inTx(() -> runRepository.failStale(threshold, STALE, LocalDateTime.now()))).isZero();

        RagEvaluationRunEntity stale = runRepository.findById(staleId).orElseThrow();
        assertThat(stale.getStatus()).isEqualTo(RagEvaluationStatus.FAILED);
        assertThat(stale.getActiveSlot()).isNull();
        assertThat(stale.getFatalError()).isEqualTo(STALE);
        assertThat(runRepository.findById(reviewedId).orElseThrow().getStatus())
                .isEqualTo(RagEvaluationStatus.REVIEW_REQUIRED);
    }

    @Test
    @DisplayName("최근 heartbeat가 있는 실행은 stale 복구 대상이 아니다")
    void recentHeartbeatIsNotStale() {
        String runId = runningRunWithHeartbeat(LocalDateTime.now().minusMinutes(1));

        int changed = inTx(() -> runRepository.failStale(
                LocalDateTime.now().minusMinutes(10), STALE, LocalDateTime.now()));

        assertThat(changed).isZero();
        assertThat(runRepository.findById(runId).orElseThrow().getStatus()).isEqualTo(RagEvaluationStatus.RUNNING);
    }

    @Test
    @DisplayName("stale로 실패 처리된 실행을 늦게 끝난 worker가 덮어쓰지 못한다")
    void lateWorkerCannotOverwriteFailedRun() {
        String runId = runningRunWithHeartbeat(LocalDateTime.now().minusMinutes(30));
        inTx(() -> runRepository.failStale(LocalDateTime.now().minusMinutes(10), STALE, LocalDateTime.now()));

        assertThat(inTx(() -> runRepository.touchHeartbeat(runId, LocalDateTime.now()))).isZero();
        assertThat(inTx(() -> runRepository.markReviewRequired(runId, "{}", LocalDateTime.now()))).isZero();
        assertThat(inTx(() -> runRepository.markFailed(runId, "late", LocalDateTime.now()))).isZero();

        RagEvaluationRunEntity run = runRepository.findById(runId).orElseThrow();
        assertThat(run.getStatus()).isEqualTo(RagEvaluationStatus.FAILED);
        assertThat(run.getFatalError()).isEqualTo(STALE);
    }

    private String runningRunWithHeartbeat(LocalDateTime heartbeatAt) {
        String runId = runRepository.saveAndFlush(queued()).getRunId();
        inTx(() -> runRepository.markRunning(runId, heartbeatAt));
        backdateHeartbeat(runId, heartbeatAt);
        return runId;
    }

    private void backdateHeartbeat(String runId, LocalDateTime heartbeatAt) {
        inTx(() -> entityManager
                .createNativeQuery("UPDATE tb_rag_evaluation_run SET heartbeat_at = ?1 WHERE run_id = ?2")
                .setParameter(1, heartbeatAt)
                .setParameter(2, runId)
                .executeUpdate());
    }

    private int inTx(IntSupplier action) {
        return transactionTemplate.execute(status -> action.getAsInt());
    }

    private static RagEvaluationRunEntity queued() {
        return RagEvaluationRunEntity.queued(
                UUID.randomUUID().toString(),
                "rag-mobile-v1.1",
                RagEvaluationSplit.DEV,
                UUID.randomUUID().toString(),
                LocalDateTime.now().plusHours(24),
                "test",
                "v1",
                "test",
                "{}"
        );
    }
}
