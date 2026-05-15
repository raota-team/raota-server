package com.raota.domain.retrieval;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.raota.domain.community.model.Post;
import com.raota.domain.community.repository.command.PostRepository;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.domain.retrieval.document.factory.PostReviewChunkDocumentFactory;
import com.raota.domain.retrieval.document.factory.RamenShopProfileDocumentFactory;
import com.raota.domain.retrieval.service.RetrievalIndexingService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

class RetrievalIndexingServiceTest {

    @Test
    void indexPost_should_add_documents_to_vector_store() {
        RamenShopRepository ramenShopRepository = mock(RamenShopRepository.class);
        RamenShopProfileDocumentFactory shopFactory = mock(RamenShopProfileDocumentFactory.class);
        PostRepository postRepository = mock(PostRepository.class);
        PostReviewChunkDocumentFactory postFactory = mock(PostReviewChunkDocumentFactory.class);
        VectorStore vectorStore = mock(VectorStore.class);

        RetrievalIndexingService service = new RetrievalIndexingService(
                ramenShopRepository,
                shopFactory,
                postRepository,
                postFactory,
                vectorStore
        );

        Post post = mock(Post.class);
        Document document = new Document("리뷰 내용", Map.of());

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postFactory.create(post)).thenReturn(List.of(document));

        service.indexPost(1L);

        verify(vectorStore).add(List.of(document));
    }
}
