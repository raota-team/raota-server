package com.raota.agent.application.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

class RagEvaluationDatasetLoaderTest {

    private RagEvaluationDatasetLoader loader;

    @BeforeEach
    void setUp() {
        loader = new RagEvaluationDatasetLoader(JsonMapper.builder().build());
        ReflectionTestUtils.setField(
                loader,
                "latestDatasetResource",
                new ClassPathResource("evaluation/rag-mobile-v1.json")
        );
        ReflectionTestUtils.setField(
                loader,
                "v11DatasetResource",
                new ClassPathResource("evaluation/rag-mobile-v1.1.json")
        );
    }

    @Test
    void keepsV11AvailableForAComparableRerun() {
        assertThat(loader.load("rag-mobile-v1.1").version()).isEqualTo("rag-mobile-v1.1");
        assertThat(loader.loadDefault().version()).isEqualTo("rag-mobile-v1.2");
    }
}
