package com.raota.application.retrieval;

import com.raota.domain.community.model.Post;
import com.raota.domain.community.repository.command.PostRepository;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.domain.retrieval.document.RetrievalDocumentSource;
import com.raota.domain.retrieval.document.RetrievalDocumentType;
import com.raota.domain.retrieval.document.RetrievalMetadataKeys;
import com.raota.domain.retrieval.document.factory.PostReviewChunkDocumentFactory;
import com.raota.domain.retrieval.document.factory.RamenShopProfileDocumentFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RetrievalIndexingService {

    private final RamenShopRepository ramenShopRepository;
    private final RamenShopProfileDocumentFactory ramenShopProfileDocumentFactory;
    private final PostRepository postRepository;
    private final PostReviewChunkDocumentFactory postReviewChunkDocumentFactory;
    private final VectorStore vectorStore;

    public void indexAllShops() {
        List<RamenShop> shops = ramenShopRepository.findAll();

        for (RamenShop shop : shops) {
            addDocuments(ramenShopProfileDocumentFactory.create(shop));
        }
    }

    public void indexShop(Long shopId) {
        RamenShop shop = ramenShopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("라멘샵을 찾을 수 없습니다. id=" + shopId));

        addDocuments(ramenShopProfileDocumentFactory.create(shop));
    }

    public void deleteAllShops() {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        var filter = builder.and(
                builder.eq(
                        RetrievalMetadataKeys.DOCUMENT_TYPE,
                        RetrievalDocumentType.SHOP_PROFILE.name()
                ),
                builder.eq(
                        RetrievalMetadataKeys.SOURCE,
                        RetrievalDocumentSource.RAMEN_SHOP.name()
                )
        ).build();

        vectorStore.delete(filter);
    }

    public void deleteShop(Long shopId) {
        if (shopId == null) {
            return;
        }

        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        var filter = builder.and(
                builder.and(
                        builder.eq(
                                RetrievalMetadataKeys.DOCUMENT_TYPE,
                                RetrievalDocumentType.SHOP_PROFILE.name()
                        ),
                        builder.eq(
                                RetrievalMetadataKeys.SOURCE,
                                RetrievalDocumentSource.RAMEN_SHOP.name()
                        )
                ),
                builder.eq(
                        RetrievalMetadataKeys.SHOP_ID,
                        String.valueOf(shopId)
                )
        ).build();

        vectorStore.delete(filter);
    }

    public void indexPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));

        RamenShop shop = post.getRamenShopId() == null
                ? null
                : ramenShopRepository.findById(post.getRamenShopId()).orElse(null);

        deletePost(postId);

        List<Document> documents = postReviewChunkDocumentFactory.create(post, shop);
        addDocuments(documents);
    }

    public void deletePost(Long postId) {
        if (postId == null) {
            return;
        }

        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        var filter = builder.and(
                builder.and(
                        builder.eq(
                                RetrievalMetadataKeys.DOCUMENT_TYPE,
                                RetrievalDocumentType.REVIEW_CHUNK.name()
                        ),
                        builder.eq(
                                RetrievalMetadataKeys.SOURCE,
                                RetrievalDocumentSource.COMMUNITY_POST.name()
                        )
                ),
                builder.eq(
                        RetrievalMetadataKeys.SOURCE_ID,
                        String.valueOf(postId)
                )
        ).build();

        vectorStore.delete(filter);
    }

    private void addDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        for (Document document : documents) {
            vectorStore.add(List.of(document));
        }
    }

}
