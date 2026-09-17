package com.raota.agent.application.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.raota.agent.application.evaluation.RagEvaluationRunService.RunStart;
import com.raota.agent.infrastructure.persistence.evaluation.RagEvaluationCaseResultJpaRepository;
import com.raota.agent.infrastructure.persistence.evaluation.RagEvaluationRunJpaRepository;
import com.raota.agent.infrastructure.persistence.evaluation.entity.RagEvaluationRunEntity;
import com.raota.global.redis.RedisLockClient;
import com.raota.global.redis.RedisLockClient.LockToken;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 실제 MySQL 제약 이름 매칭은 RagEvaluationRunConcurrencyIntegrationTest가 검증한다.
 * 여기서는 저장 오류 종류에 따른 분기만 Spring 컨텍스트 없이 확인한다.
 */
class RagEvaluationRunServiceReservationErrorTest {

    private static final String KEY = "reservation-error-key";
    private static final LockToken LOCK = new LockToken("lock:rag-evaluation:active", "token");

    private final RagEvaluationDatasetLoader datasetLoader = mock(RagEvaluationDatasetLoader.class);
    private final RagEvaluationRunJpaRepository runRepository = mock(RagEvaluationRunJpaRepository.class);
    private final RagEvaluationRunner runner = mock(RagEvaluationRunner.class);
    private final RedisLockClient lockClient = mock(RedisLockClient.class);
    private RagEvaluationRunService runService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        RagEvaluationDataset dataset = mock(RagEvaluationDataset.class);
        given(dataset.version()).willReturn("rag-mobile-v1.1");
        given(datasetLoader.load(any())).willReturn(dataset);
        given(lockClient.tryAcquire(anyString(), any(Duration.class))).willReturn(Optional.of(LOCK));
        given(lockClient.release(LOCK)).willReturn(true);
        given(runRepository.findFirstByStatusInOrderByCreatedAtDesc(anyCollection())).willReturn(Optional.empty());
        runService = new RagEvaluationRunService(
                datasetLoader,
                runRepository,
                mock(RagEvaluationCaseResultJpaRepository.class),
                runner,
                lockClient,
                new TransactionTemplate(mock(PlatformTransactionManager.class)),
                objectMapper,
                "test", "v1", "test", "{}"
        );
    }

    @Test
    @DisplayName("활성 슬롯과 무관한 제약 위반은 이미 실행 중 오류로 바꾸지 않고 그대로 던진다")
    void unrelatedViolationIsRethrown() {
        DataIntegrityViolationException dataTooLong = violation("Data too long for column 'server_commit' at row 1");
        given(runRepository.findByIdempotencyKey(KEY)).willReturn(Optional.empty());
        given(runRepository.saveAndFlush(any())).willThrow(dataTooLong);

        assertThatThrownBy(() -> runService.start(null, RagEvaluationSplit.DEV, KEY)).isSameAs(dataTooLong);
        verify(lockClient).release(LOCK);
        verify(runner, never()).execute(anyString(), any(), any());
    }

    @Test
    @DisplayName("활성 슬롯 UNIQUE 충돌은 이미 실행 중 오류로 바꾼다")
    void activeSlotViolationBecomesAlreadyRunning() {
        given(runRepository.findByIdempotencyKey(KEY)).willReturn(Optional.empty());
        given(runRepository.saveAndFlush(any())).willThrow(violation(
                "Duplicate entry 'ACTIVE' for key 'tb_rag_evaluation_run.uk_rag_evaluation_run_active_slot'"));

        assertThatThrownBy(() -> runService.start(null, RagEvaluationSplit.DEV, KEY))
                .isInstanceOf(RagEvaluationAlreadyRunningException.class);
        verify(lockClient).release(LOCK);
    }

    @Test
    @DisplayName("멱등키 UNIQUE 충돌은 먼저 저장된 실행을 다시 조회해 반환한다")
    void idempotencyKeyViolationReturnsExistingRun() {
        RagEvaluationRunEntity existing = RagEvaluationRunEntity.queued(
                "existing-run", "rag-mobile-v1.1", RagEvaluationSplit.DEV, KEY,
                LocalDateTime.now().plusHours(24), "test", "v1", "test", "{}");
        given(runRepository.findByIdempotencyKey(KEY))
                .willReturn(Optional.empty(), Optional.empty(), Optional.of(existing));
        given(runRepository.saveAndFlush(any())).willThrow(violation(
                "Duplicate entry '" + KEY + "' for key 'tb_rag_evaluation_run.uk_rag_evaluation_run_idempotency_key'"));

        RunStart result = runService.start(null, RagEvaluationSplit.DEV, KEY);

        assertThat(result.runId()).isEqualTo("existing-run");
        assertThat(result.idempotentReplay()).isTrue();
    }

    private static DataIntegrityViolationException violation(String databaseMessage) {
        return new DataIntegrityViolationException(
                "could not execute statement",
                new SQLIntegrityConstraintViolationException(databaseMessage)
        );
    }
}
