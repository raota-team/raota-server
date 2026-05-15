package com.raota.domain.retrieval.document.review;

import com.raota.domain.community.model.Post;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.domain.retrieval.document.RetrievalDocumentFactory;
import com.raota.domain.retrieval.document.RetrievalDocumentSource;
import com.raota.domain.retrieval.document.RetrievalDocumentType;
import com.raota.domain.retrieval.document.RetrievalMetadataKeys;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostReviewChunkDocumentFactory implements RetrievalDocumentFactory<Post> {

    private final RamenShopRepository ramenShopRepository;

    @Override
    public List<Document> create(Post post) {
        if (post == null) {
            return List.of();
        }

        String content = buildContent(post);
        if (content.isBlank()) {
            return List.of();
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(RetrievalMetadataKeys.DOCUMENT_TYPE, RetrievalDocumentType.REVIEW_CHUNK.name());
        metadata.put(RetrievalMetadataKeys.SOURCE, RetrievalDocumentSource.COMMUNITY_POST.name());
        metadata.put(RetrievalMetadataKeys.SOURCE_ID, String.valueOf(post.getId()));

        if (post.getRamenShopId() != null) {
            RamenShop shop = ramenShopRepository.findById(post.getRamenShopId()).orElse(null);
            if (shop != null) {
                metadata.put(RetrievalMetadataKeys.SHOP_ID, String.valueOf(shop.getId()));
                metadata.put(RetrievalMetadataKeys.SHOP_NAME, shop.getName());
                metadata.put(RetrievalMetadataKeys.REGION, shop.getAddress() != null ? shop.getAddress().simpleAddress() : "위치 정보 없음");
            }
        }

        if (post.getCreatedAt() != null) {
            metadata.put(RetrievalMetadataKeys.CREATED_AT, post.getCreatedAt().toString());
        }

        return List.of(new Document(content, metadata));
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