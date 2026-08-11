package com.raota.agent.infrastructure.vector;

import com.raota.agent.application.ramenshop.port.RamenShopComparisonDocumentPort;
import com.raota.agent.application.ramenshop.result.RamenShopComparisonDocument;
import com.raota.agent.domain.retrieval.document.RetrievalDocumentFilters;
import java.util.List;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

@Component
public class OracleRamenShopComparisonDocumentAdapter implements RamenShopComparisonDocumentPort {

    private final VectorStore vectorStore;

    public OracleRamenShopComparisonDocumentAdapter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<RamenShopComparisonDocument> searchComparisonDocuments(
            Long shopId,
            String query,
            int topK,
            double similarityThreshold
    ) {
        var documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .filterExpression(RetrievalDocumentFilters.shopProfileOrExternalReviewsForShop(shopId))
                        .build()
        );

        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        return documents.stream()
                .map(document -> new RamenShopComparisonDocument(
                        document.getText(),
                        document.getMetadata()
                ))
                .toList();
    }
}
