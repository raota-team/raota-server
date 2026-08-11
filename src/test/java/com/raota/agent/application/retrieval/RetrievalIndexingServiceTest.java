package com.raota.agent.application.retrieval;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.raota.agent.application.retrieval.RetrievalIndexingService;
import com.raota.community.domain.model.Post;
import com.raota.community.domain.repository.PostRepository;
import com.raota.ramenshop.domain.repository.RamenShopRepository;
import com.raota.agent.domain.retrieval.document.factory.PostReviewChunkDocumentFactory;
import com.raota.agent.domain.retrieval.document.factory.RamenShopProfileDocumentFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.jdbc.core.JdbcTemplate;

class RetrievalIndexingServiceTest {

    @Test
    void indexPost_should_add_documents_to_vector_store() {
        RamenShopRepository ramenShopRepository = mock(RamenShopRepository.class);
        RamenShopProfileDocumentFactory shopFactory = mock(RamenShopProfileDocumentFactory.class);
        PostRepository postRepository = mock(PostRepository.class);
        PostReviewChunkDocumentFactory postFactory = mock(PostReviewChunkDocumentFactory.class);
        VectorStore vectorStore = mock(VectorStore.class);

        RetrievalIndexingService service = createService(
                ramenShopRepository,
                shopFactory,
                postRepository,
                postFactory,
                vectorStore
        );

        Post post = mock(Post.class);
        Document document = new Document("리뷰 내용", Map.of());

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postFactory.create(post, null)).thenReturn(List.of(document));

        service.indexPost(1L);

        verify(vectorStore).delete(org.mockito.ArgumentMatchers.any(Filter.Expression.class));
        verify(vectorStore).add(List.of(document));
    }

    @Test
    void deletePost_should_delete_review_chunks_by_filter() {
        RamenShopRepository ramenShopRepository = mock(RamenShopRepository.class);
        RamenShopProfileDocumentFactory shopFactory = mock(RamenShopProfileDocumentFactory.class);
        PostRepository postRepository = mock(PostRepository.class);
        PostReviewChunkDocumentFactory postFactory = mock(PostReviewChunkDocumentFactory.class);
        VectorStore vectorStore = mock(VectorStore.class);

        RetrievalIndexingService service = createService(
                ramenShopRepository,
                shopFactory,
                postRepository,
                postFactory,
                vectorStore
        );

        service.deletePost(1L);

        verify(vectorStore).delete(org.mockito.ArgumentMatchers.any(Filter.Expression.class));
    }

    @Test
    void deletePost_should_ignore_null_post_id() {
        RamenShopRepository ramenShopRepository = mock(RamenShopRepository.class);
        RamenShopProfileDocumentFactory shopFactory = mock(RamenShopProfileDocumentFactory.class);
        PostRepository postRepository = mock(PostRepository.class);
        PostReviewChunkDocumentFactory postFactory = mock(PostReviewChunkDocumentFactory.class);
        VectorStore vectorStore = mock(VectorStore.class);

        RetrievalIndexingService service = createService(
                ramenShopRepository,
                shopFactory,
                postRepository,
                postFactory,
                vectorStore
        );

        service.deletePost(null);

        verifyNoInteractions(vectorStore);
    }

    private RetrievalIndexingService createService(
            RamenShopRepository ramenShopRepository,
            RamenShopProfileDocumentFactory shopFactory,
            PostRepository postRepository,
            PostReviewChunkDocumentFactory postFactory,
            VectorStore vectorStore
    ) {
        return new RetrievalIndexingService(
                ramenShopRepository,
                shopFactory,
                postRepository,
                postFactory,
                vectorStore,
                mock(EmbeddingModel.class),
                mock(JdbcTemplate.class)
        );
    }
}
