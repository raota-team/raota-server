package com.raota.agent.application.evaluation;

import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RagEvaluationDatasetLoader {

    private final ObjectMapper objectMapper;

    @Value("classpath:/evaluation/rag-mobile-v1.json")
    private Resource datasetResource;

    public RagEvaluationDataset loadDefault() {
        try (InputStream inputStream = datasetResource.getInputStream()) {
            return objectMapper.readValue(inputStream, RagEvaluationDataset.class);
        } catch (IOException exception) {
            throw new IllegalStateException("RAG 평가셋을 읽을 수 없습니다.", exception);
        }
    }
}
