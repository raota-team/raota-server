package com.raota.domain.retrieval.document.factory;

import com.raota.domain.community.model.Post;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.retrieval.document.RetrievalDocumentFactory;
import com.raota.domain.retrieval.document.RetrievalDocumentSource;
import com.raota.domain.retrieval.document.RetrievalDocumentType;
import com.raota.domain.retrieval.document.RetrievalMetadataKeys;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

@Component
public class PostReviewChunkDocumentFactory implements RetrievalDocumentFactory<Post> {

    private static final int MIN_INDEXABLE_TEXT_LENGTH = 30;

    private final TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder().build();

    @Override
    public List<Document> create(Post post) {
        return create(post, null);
    }

    public List<Document> create(Post post, RamenShop shop) {
        if (post == null) {
            return List.of();
        }

        String content = buildContent(post);
        if (content.isBlank() || normalizedLength(content) < MIN_INDEXABLE_TEXT_LENGTH) {
            return List.of();
        }

        Map<String, Object> metadata = buildMetadata(post, shop);
        Document document = new Document(content, metadata);

        List<Document> splitDocuments = tokenTextSplitter.split(document);

        return IntStream.range(0, splitDocuments.size())
                .mapToObj(index -> withChunkMetadata(
                        splitDocuments.get(index),
                        post.getId(),
                        index,
                        splitDocuments.size()
                ))
                .toList();
    }

    private int normalizedLength(String content) {
        return content.replaceAll("\\s+", "").length();
    }

    private Document withChunkMetadata(Document document, Long postId, int chunkIndex, int chunkTotal) {
        Map<String, Object> metadata = new HashMap<>(document.getMetadata());
        metadata.put(RetrievalMetadataKeys.CHUNK_INDEX, chunkIndex);
        metadata.put(RetrievalMetadataKeys.CHUNK_TOTAL, chunkTotal);
        metadata.put(RetrievalMetadataKeys.CHUNK_ID,
                "post:%s:chunk:%d".formatted(postId, chunkIndex));

        return new Document(document.getText(), metadata);
    }

    private Map<String, Object> buildMetadata(Post post, RamenShop shop) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(RetrievalMetadataKeys.DOCUMENT_TYPE, RetrievalDocumentType.REVIEW_CHUNK.name());
        metadata.put(RetrievalMetadataKeys.SOURCE, RetrievalDocumentSource.COMMUNITY_POST.name());
        metadata.put(RetrievalMetadataKeys.SOURCE_ID, String.valueOf(post.getId()));

        if (shop != null) {
            metadata.put(RetrievalMetadataKeys.SHOP_ID, String.valueOf(shop.getId()));
            metadata.put(RetrievalMetadataKeys.SHOP_NAME, shop.getName());
            metadata.put(RetrievalMetadataKeys.REGION, shop.getAddress() != null ? shop.getAddress().simpleAddress() : "위치 정보 없음");
        }

        if (post.getCreatedAt() != null) {
            metadata.put(RetrievalMetadataKeys.CREATED_AT, post.getCreatedAt().toString());
        }

        return metadata;
    }

    private String buildContent(Post post) {
        String title = defaultText(post.getTitle(), "");
        String body = defaultText(post.getContent(), "");

        String combined = (title + "\n" + body).trim();
        if (combined.isBlank()) {
            return "";
        }
        return combined;
    }

    private String defaultText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
