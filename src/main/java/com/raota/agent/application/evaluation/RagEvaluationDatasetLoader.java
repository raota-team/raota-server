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

    private static final String VERSION_V11 = "rag-mobile-v1.1";

    private final ObjectMapper objectMapper;

    @Value("classpath:/evaluation/rag-mobile-v1.json")
    private Resource latestDatasetResource;

    @Value("classpath:/evaluation/rag-mobile-v1.1.json")
    private Resource v11DatasetResource;

    public RagEvaluationDataset loadDefault() {
        return read(latestDatasetResource);
    }

    public RagEvaluationDataset load(String version) {
        if (version == null || version.isBlank()) {
            return loadDefault();
        }
        return switch (version) {
            case VERSION_V11 -> read(v11DatasetResource);
            case "rag-mobile-v1.2" -> loadDefault();
            default -> throw new IllegalArgumentException("지원하지 않는 평가셋 버전입니다: " + version);
        };
    }

    private RagEvaluationDataset read(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, RagEvaluationDataset.class);
        }
        catch (IOException exception) {
            throw new IllegalStateException("RAG 평가셋을 읽을 수 없습니다.", exception);
        }
    }

}
