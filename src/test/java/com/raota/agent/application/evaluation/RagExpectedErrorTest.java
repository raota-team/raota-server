package com.raota.agent.application.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;

class RagExpectedErrorTest {

    @Test
    void classifiesMatchingValidationErrorWithoutCallingItFallback() {
        RagExecutionResult actual = RagExecutionResult.error(0, new IllegalArgumentException("서로 다른 두 매장을 선택해야 합니다."));
        RagExpectedError expected = new RagExpectedError("validation_error", 400, "서로 다른 두 매장을 선택해야 합니다.");

        assertThat(expected.matches(actual)).isTrue();
        assertThat(actual.asExpectedError().status()).isEqualTo(RagEvaluationCaseStatus.EXPECTED_ERROR);
        assertThat(actual.asExpectedError().fallback()).isFalse();
        assertThat(RagEvaluationMetricCalculator.calculateExpectedError(true)).containsEntry("expectedErrorMatch", 1.0);
    }

    @Test
    void doesNotClassifyDifferentExceptionAsResourceNotFound() {
        RagExecutionResult actual = RagExecutionResult.error(0, new IllegalArgumentException("서로 다른 두 매장을 선택해야 합니다."));
        RagExpectedError expected = new RagExpectedError("RESOURCE_NOT_FOUND", 404, "라멘샵을 찾을 수 없습니다.");

        assertThat(expected.matches(actual)).isFalse();
    }

    @Test
    void classifiesPublishedShopNotFoundAsResourceNotFound() {
        RagExecutionResult actual = RagExecutionResult.error(0,
                new EntityNotFoundException("라멘샵을 찾을 수 없습니다. id=999999"));
        RagExpectedError expected = new RagExpectedError("RESOURCE_NOT_FOUND", 404, "라멘샵을 찾을 수 없습니다.");

        assertThat(expected.matches(actual)).isTrue();
    }

}
