package com.raota.agent.application.evaluation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.raota.ramenshop.domain.repository.RamenShopRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class RagEvaluationDatasetReferenceValidatorTest {

    private final RamenShopRepository shopRepository = mock(RamenShopRepository.class);
    private final RagEvaluationDatasetReferenceValidator validator =
            new RagEvaluationDatasetReferenceValidator(shopRepository);
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void 운영에없는_일반_사례_매장참조는_실행전에_실패한다() {
        RagEvaluationCase evaluationCase = new RagEvaluationCase(
                "summary-1", RagEvaluationCaseType.SUMMARY, RagEvaluationSplit.DEV,
                objectMapper.createObjectNode().put("shopId", 1), "AUTHENTICATED",
                List.of(), false, List.of(), List.of(), List.of(), false, false, 1, 6
        );
        RagEvaluationDataset dataset = new RagEvaluationDataset("rag-mobile-test", List.of(evaluationCase));
        when(shopRepository.findByIdAndPublishedTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validate(dataset, RagEvaluationSplit.DEV))
                .isInstanceOf(RagEvaluationSafetyException.class)
                .hasMessageContaining("summary-1(shopId=1)");
    }

    @Test
    void fallback_사례의_의도적인_없는_매장참조는_허용한다() {
        RagEvaluationCase evaluationCase = new RagEvaluationCase(
                "summary-fallback", RagEvaluationCaseType.SUMMARY, RagEvaluationSplit.DEV,
                objectMapper.createObjectNode().put("shopId", 999999), "AUTHENTICATED",
                List.of(), false, List.of(), List.of(), List.of(), true, false, 1, 6
        );
        RagEvaluationDataset dataset = new RagEvaluationDataset("rag-mobile-test", List.of(evaluationCase));

        validator.validate(dataset, RagEvaluationSplit.DEV);
    }

    @Test
    void expectedError_사례의_없는_매장참조는_허용한다() {
        RagEvaluationCase evaluationCase = new RagEvaluationCase(
                "summary-not-found", RagEvaluationCaseType.SUMMARY, RagEvaluationSplit.DEV,
                objectMapper.createObjectNode().put("shopId", 999999), "AUTHENTICATED",
                List.of(), false, List.of(), List.of(), List.of(), false, false, 1, 6,
                new RagExpectedError("RESOURCE_NOT_FOUND", 404, "라멘샵을 찾을 수 없습니다.")
        );
        RagEvaluationDataset dataset = new RagEvaluationDataset("rag-mobile-test", List.of(evaluationCase));

        validator.validate(dataset, RagEvaluationSplit.DEV);
    }
}
