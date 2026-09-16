package com.raota.agent.application.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class RagEvaluationDatasetTest {

    @Test
    void resourceContainsTheFrozenThirtyCaseMobileSplit() throws Exception {
        ObjectMapper mapper = JsonMapper.builder().build();
        try (InputStream stream = new ClassPathResource("evaluation/rag-mobile-v1.json").getInputStream()) {
            RagEvaluationDataset dataset = mapper.readValue(stream, RagEvaluationDataset.class);

            assertThat(dataset.version()).isEqualTo("rag-mobile-v1.1");
            assertThat(dataset.cases()).hasSize(30);
            assertThat(dataset.casesFor(RagEvaluationSplit.DEV)).hasSize(20);
            assertThat(dataset.casesFor(RagEvaluationSplit.HOLDOUT)).hasSize(10);
            assertThat(dataset.countsByType())
                    .containsEntry(RagEvaluationCaseType.SEARCH, 12L)
                    .containsEntry(RagEvaluationCaseType.SUMMARY, 6L)
                    .containsEntry(RagEvaluationCaseType.CHAT, 6L)
                    .containsEntry(RagEvaluationCaseType.COMPARE, 6L);
            assertThat(dataset.cases()).filteredOn(RagEvaluationCase::contractOnly)
                    .extracting(RagEvaluationCase::caseId)
                    .containsExactlyInAnyOrder("search-dev-06", "search-holdout-04");
            assertThat(dataset.cases()).allSatisfy(item -> {
                assertThat(item.primaryK()).isEqualTo(1);
                assertThat(item.diagnosticK()).isEqualTo(6);
            });
        }
    }
}
