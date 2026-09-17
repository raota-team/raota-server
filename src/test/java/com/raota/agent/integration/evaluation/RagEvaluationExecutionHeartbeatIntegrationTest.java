package com.raota.agent.integration.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.raota.agent.application.evaluation.RagEvaluationCaseExecutor;
import com.raota.agent.application.evaluation.RagEvaluationDataset;
import com.raota.agent.application.evaluation.RagEvaluationDatasetLoader;
import com.raota.agent.application.evaluation.RagEvaluationDatasetReferenceValidator;
import com.raota.agent.application.evaluation.RagEvaluationRunner;
import com.raota.agent.application.evaluation.RagEvaluationSplit;
import com.raota.agent.application.evaluation.RagEvaluationStatus;
import com.raota.agent.application.evaluation.RagExecutionResult;
import com.raota.agent.infrastructure.persistence.evaluation.RagEvaluationCaseResultJpaRepository;
import com.raota.agent.infrastructure.persistence.evaluation.RagEvaluationRunJpaRepository;
import com.raota.agent.infrastructure.persistence.evaluation.entity.RagEvaluationRunEntity;
import com.raota.support.BaseIntegrationTest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

@TestPropertySource(properties = "app.rag.evaluation.heartbeat-interval=PT0.1S")
class RagEvaluationExecutionHeartbeatIntegrationTest extends BaseIntegrationTest {

    private static final Duration WAIT = Duration.ofSeconds(10);

    @MockitoBean
    private RagEvaluationCaseExecutor caseExecutor;

    @MockitoBean
    private RagEvaluationDatasetReferenceValidator referenceValidator;

    @Autowired
    private RagEvaluationRunner runner;

    @Autowired
    private RagEvaluationDatasetLoader datasetLoader;

    @MockitoSpyBean
    private RagEvaluationRunJpaRepository runRepository;

    @Autowired
    private RagEvaluationCaseResultJpaRepository caseRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final CountDownLatch releaseFirstCase = new CountDownLatch(1);

    @BeforeEach
    void setUp() {
        clean();
        given(caseExecutor.execute(any())).willAnswer(invocation -> {
            releaseFirstCase.await(WAIT.toSeconds(), TimeUnit.SECONDS);
            return RagExecutionResult.error(1, new IllegalStateException("test case error"));
        });
    }

    @AfterEach
    void tearDown() {
        releaseFirstCase.countDown();
        Awaitility.await().atMost(WAIT).until(() -> runRepository.findAll().stream()
                .noneMatch(run -> run.getStatus() == RagEvaluationStatus.RUNNING));
        clean();
    }

    @Test
    @DisplayName("실행 중에는 heartbeat가 갱신되고 끝나면 활성 슬롯을 비운다")
    void heartbeatAdvancesWhileRunning() {
        String runId = saveQueuedRun();
        RagEvaluationDataset dataset = datasetLoader.loadDefault();

        runner.execute(runId, dataset, RagEvaluationSplit.DEV);
        Awaitility.await().atMost(WAIT).until(() -> status(runId) == RagEvaluationStatus.RUNNING);
        LocalDateTime firstHeartbeat = run(runId).getHeartbeatAt();

        Awaitility.await().atMost(WAIT).until(() -> run(runId).getHeartbeatAt().isAfter(firstHeartbeat));
        releaseFirstCase.countDown();

        Awaitility.await().atMost(WAIT).until(() -> status(runId) == RagEvaluationStatus.REVIEW_REQUIRED);
        assertThat(run(runId).getActiveSlot()).isNull();
    }

    @Test
    @DisplayName("다른 서버가 실패 처리한 실행은 worker가 다음 사례 전에 중단하고 상태를 덮어쓰지 않는다")
    void workerStopsWhenRunBecomesInactive() {
        String runId = saveQueuedRun();

        runner.execute(runId, datasetLoader.loadDefault(), RagEvaluationSplit.DEV);
        Awaitility.await().atMost(WAIT).until(() -> status(runId) == RagEvaluationStatus.RUNNING);
        verifyExecutorCalled();
        transactionTemplate.execute(status -> runRepository.markFailed(runId, "STALE_HEARTBEAT", LocalDateTime.now()));
        Awaitility.await().pollDelay(Duration.ofMillis(400)).atMost(WAIT).until(() -> true);

        releaseFirstCase.countDown();

        Awaitility.await().atMost(WAIT).until(() -> caseRepository.findByRunIdOrderByIdAsc(runId).stream()
                .noneMatch(item -> item.getStatus().name().equals("RUNNING")));
        verify(caseExecutor, times(1)).execute(any());
        RagEvaluationRunEntity run = run(runId);
        assertThat(run.getStatus()).isEqualTo(RagEvaluationStatus.FAILED);
        assertThat(run.getFatalError()).isEqualTo("STALE_HEARTBEAT");
    }

    @Test
    @DisplayName("QUEUED가 아닌 실행은 worker가 시작하지 않는다")
    void doesNotStartRunThatIsNotQueued() throws InterruptedException {
        String runId = saveQueuedRun();
        transactionTemplate.execute(status -> runRepository.markFailed(runId, "cancelled", LocalDateTime.now()));

        runner.execute(runId, datasetLoader.loadDefault(), RagEvaluationSplit.DEV);
        TimeUnit.MILLISECONDS.sleep(300);

        verify(caseExecutor, never()).execute(any());
        assertThat(status(runId)).isEqualTo(RagEvaluationStatus.FAILED);
    }

    @Test
    @DisplayName("시작 전환 중 DB 오류가 나면 실행을 FAILED로 바꿔 활성 슬롯을 즉시 반환한다")
    void startTransitionFailureMarksRunFailed() {
        String runId = saveQueuedRun();
        doThrow(new DataAccessResourceFailureException("database unavailable"))
                .when(runRepository).markRunning(anyString(), any(LocalDateTime.class));

        runner.execute(runId, datasetLoader.loadDefault(), RagEvaluationSplit.DEV);

        Awaitility.await().atMost(WAIT).until(() -> status(runId) == RagEvaluationStatus.FAILED);
        RagEvaluationRunEntity run = run(runId);
        assertThat(run.getActiveSlot()).isNull();
        assertThat(run.getFatalError()).isEqualTo("database unavailable");
        verify(caseExecutor, never()).execute(any());
    }

    private void verifyExecutorCalled() {
        Awaitility.await().atMost(WAIT).untilAsserted(() -> verify(caseExecutor, atLeastOnce()).execute(any()));
    }

    private String saveQueuedRun() {
        return runRepository.saveAndFlush(RagEvaluationRunEntity.queued(
                UUID.randomUUID().toString(),
                datasetLoader.loadDefault().version(),
                RagEvaluationSplit.DEV,
                UUID.randomUUID().toString(),
                LocalDateTime.now().plusHours(24),
                "test",
                "v1",
                "test",
                "{}"
        )).getRunId();
    }

    private RagEvaluationRunEntity run(String runId) {
        return runRepository.findById(runId).orElseThrow();
    }

    private RagEvaluationStatus status(String runId) {
        return run(runId).getStatus();
    }

    private void clean() {
        caseRepository.deleteAllInBatch();
        runRepository.deleteAllInBatch();
    }
}
