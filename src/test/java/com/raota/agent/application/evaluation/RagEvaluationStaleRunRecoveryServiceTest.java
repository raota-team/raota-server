package com.raota.agent.application.evaluation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.raota.agent.infrastructure.persistence.evaluation.RagEvaluationRunJpaRepository;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RagEvaluationStaleRunRecoveryServiceTest {

    private final RagEvaluationRunJpaRepository repository = mock(RagEvaluationRunJpaRepository.class);

    @Test
    @DisplayName("stale 임계값이 heartbeat 주기의 3배보다 짧으면 기동을 거부한다")
    void rejectsThresholdTooCloseToHeartbeat() {
        assertThatThrownBy(() -> new RagEvaluationStaleRunRecoveryService(repository, Duration.ofSeconds(80),
                Duration.ofSeconds(30)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("기본값 10분과 30초 조합은 허용한다")
    void acceptsDefaults() {
        assertThatCode(() -> new RagEvaluationStaleRunRecoveryService(repository, Duration.ofMinutes(10),
                Duration.ofSeconds(30)))
            .doesNotThrowAnyException();
    }

}
