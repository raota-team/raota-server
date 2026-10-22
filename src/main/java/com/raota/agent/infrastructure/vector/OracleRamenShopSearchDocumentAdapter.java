package com.raota.agent.infrastructure.vector;

import com.raota.agent.application.ramenshop.port.RamenShopSearchDocumentPort;
import com.raota.agent.application.ramenshop.result.RamenShopSearchDocument;
import com.raota.agent.domain.retrieval.document.RetrievalDocumentType;
import com.raota.agent.domain.retrieval.document.RetrievalMetadataKeys;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

@Component
public class OracleRamenShopSearchDocumentAdapter implements RamenShopSearchDocumentPort {

    private static final int REVIEW_CHUNKS_PER_SHOP = 5;

    private final VectorStore vectorStore;

    public OracleRamenShopSearchDocumentAdapter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<RamenShopSearchDocument> searchShopDocuments(String query, int topK, double similarityThreshold) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        List<Document> documents = new ArrayList<>();
        documents.addAll(search(query, topK, similarityThreshold, buildShopProfileFilter(builder)));
        documents.addAll(search(query, topK * 2, similarityThreshold, buildReviewFilter(builder)));

        if (documents.isEmpty()) {
            return List.of();
        }

        return documents.stream()
            .map(document -> new RamenShopSearchDocument(document.getText(), document.getMetadata(),
                    document.getScore()))
            .toList();
    }

    /** 매장 소개는 매장마다 하나라 매장 수만큼, 리뷰 조각은 매장마다 여러 개라 그 몇 배를 읽어 매장별 최고 유사도를 고른다. */
    @Override
    public Map<Long, Double> shopSimilarities(String query, Collection<Long> shopIds) {
        if (shopIds.isEmpty()) {
            return Map.of();
        }
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        List<Object> ids = shopIds.stream().map(id -> (Object) String.valueOf(id)).toList();
        List<Document> documents = new ArrayList<>();
        documents.addAll(search(query, shopIds.size(), SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL,
                builder
                    .and(builder.eq(RetrievalMetadataKeys.DOCUMENT_TYPE, RetrievalDocumentType.SHOP_PROFILE.name()),
                            builder.in(RetrievalMetadataKeys.SHOP_ID, ids))
                    .build()));
        documents.addAll(search(query, shopIds.size() * REVIEW_CHUNKS_PER_SHOP,
                SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL,
                builder.and(builder.group(builder.or(
                        builder.eq(RetrievalMetadataKeys.DOCUMENT_TYPE, RetrievalDocumentType.REVIEW_CHUNK.name()),
                        builder.eq(RetrievalMetadataKeys.DOCUMENT_TYPE,
                                RetrievalDocumentType.EXTERNAL_REVIEW_CHUNK.name()))),
                        builder.in(RetrievalMetadataKeys.SHOP_ID, ids))
                    .build()));
        Map<Long, Double> best = new HashMap<>();
        for (Document document : documents) {
            Long shopId = shopId(document.getMetadata().get(RetrievalMetadataKeys.SHOP_ID));
            if (shopId == null || !shopIds.contains(shopId) || document.getScore() == null) {
                continue;
            }
            best.merge(shopId, document.getScore(), Math::max);
        }
        return best;
    }

    private static Long shopId(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.toString().trim().replace("\"", "");
        if (value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }

    private List<Document> search(String query, int topK, double similarityThreshold, Filter.Expression filter) {
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(similarityThreshold)
            .filterExpression(filter)
            .build());
        return documents == null ? List.of() : documents;
    }

    private Filter.Expression buildShopProfileFilter(FilterExpressionBuilder builder) {
        return builder.eq(RetrievalMetadataKeys.DOCUMENT_TYPE, RetrievalDocumentType.SHOP_PROFILE.name()).build();
    }

    private Filter.Expression buildReviewFilter(FilterExpressionBuilder builder) {
        return builder
            .or(builder.eq(RetrievalMetadataKeys.DOCUMENT_TYPE, RetrievalDocumentType.REVIEW_CHUNK.name()),
                    builder.eq(RetrievalMetadataKeys.DOCUMENT_TYPE, RetrievalDocumentType.EXTERNAL_REVIEW_CHUNK.name()))
            .build();
    }

}
