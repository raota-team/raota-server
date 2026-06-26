package com.raota.infrastructure.vector;

import com.raota.application.ramenShop.port.RamenShopSearchDocumentPort;
import com.raota.application.ramenShop.result.RamenShopSearchDocument;
import com.raota.domain.retrieval.document.RetrievalDocumentType;
import com.raota.domain.retrieval.document.RetrievalMetadataKeys;
import java.util.List;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

@Component
public class OracleRamenShopSearchDocumentAdapter implements RamenShopSearchDocumentPort {

    private final VectorStore vectorStore;

    public OracleRamenShopSearchDocumentAdapter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<RamenShopSearchDocument> searchShopProfiles(String query, int topK, double similarityThreshold) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        var documents = search(query, topK, similarityThreshold, buildShopProfileFilter(builder));

        if (documents == null || documents.isEmpty()) {
            documents = search(query, topK, similarityThreshold, buildShopLinkedDocumentFilter(builder));
        }

        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        return documents.stream()
                .map(document -> new RamenShopSearchDocument(
                        document.getText(),
                        document.getMetadata(),
                        document.getScore()
                ))
                .toList();
    }

    private List<org.springframework.ai.document.Document> search(
            String query,
            int topK,
            double similarityThreshold,
            Filter.Expression filter
    ) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .filterExpression(filter)
                        .build()
        );
    }

    private Filter.Expression buildShopProfileFilter(FilterExpressionBuilder builder) {
        return builder.eq(
                RetrievalMetadataKeys.DOCUMENT_TYPE,
                RetrievalDocumentType.SHOP_PROFILE.name()
        ).build();
    }

    private Filter.Expression buildShopLinkedDocumentFilter(FilterExpressionBuilder builder) {
        return builder.or(
                builder.eq(RetrievalMetadataKeys.DOCUMENT_TYPE, RetrievalDocumentType.SHOP_PROFILE.name()),
                builder.or(
                        builder.eq(RetrievalMetadataKeys.DOCUMENT_TYPE, RetrievalDocumentType.REVIEW_CHUNK.name()),
                        builder.eq(RetrievalMetadataKeys.DOCUMENT_TYPE, RetrievalDocumentType.EXTERNAL_REVIEW_CHUNK.name())
                )
        ).build();
    }
}
