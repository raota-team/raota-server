package com.raota.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class OracleVectorStoreSmokeTest {

    @Autowired
    private VectorStore vectorStore;

    @Test
    void vector_store_add_and_search() {
        String testRunId = UUID.randomUUID().toString();

        List<Document> documents = List.of(
                new Document(
                        "테스트 런 " + testRunId + " - 진한 돈코츠 국물과 차슈가 강점인 라멘집",
                        Map.of(
                                "shopId", "1",
                                "source", "TEST",
                                "documentType", "PROFILE",
                                "testRunId", testRunId
                        )
                ),
                new Document(
                        "테스트 런 " + testRunId + " - 깔끔한 쇼유 베이스와 가벼운 식사가 장점인 라멘집",
                        Map.of(
                                "shopId", "2",
                                "source", "TEST",
                                "documentType", "PROFILE",
                                "testRunId", testRunId
                        )
                )
        );

        vectorStore.add(documents);

        List<Document> results = vectorStore.similaritySearch("진한 국물 라멘 추천");

        assertThat(results)
                .isNotEmpty()
                .extracting(Document::getText)
                .anyMatch(text -> text.contains("돈코츠"));

        assertThat(results)
                .extracting(Document::getText)
                .anyMatch(text -> text.contains(testRunId) && text.contains("돈코츠"));
    }
}
