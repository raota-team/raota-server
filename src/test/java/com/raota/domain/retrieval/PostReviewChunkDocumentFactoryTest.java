package com.raota.domain.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.raota.domain.community.model.Post;
import com.raota.domain.community.model.PostCategory;
import com.raota.domain.ramenShop.model.Address;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.domain.retrieval.document.review.PostReviewChunkDocumentFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class PostReviewChunkDocumentFactoryTest {

    @Test
    void create_should_build_review_chunk_document() {
        RamenShopRepository ramenShopRepository = mock(RamenShopRepository.class);
        PostReviewChunkDocumentFactory factory = new PostReviewChunkDocumentFactory(ramenShopRepository);

        RamenShop shop = RamenShop.builder()
                .id(1L)
                .name("멘야하나비")
                .address(Address.of("서울", "마포구", "양화로", "1"))
                .build();

        when(ramenShopRepository.findById(1L)).thenReturn(Optional.of(shop));

        Post post = Post.of(
                10L,
                PostCategory.REVIEW,
                "국물이 진했음",
                "차슈가 부드럽고 웨이팅이 길었다.",
                "TEXT",
                null,
                100L,
                1L,
                LocalDateTime.now()
        );

        List<Document> documents = factory.create(post);

        assertThat(documents).hasSize(1);

        Document document = documents.getFirst();
        assertThat(document.getText()).contains("국물이 진했음");
        assertThat(document.getText()).contains("차슈가 부드럽고 웨이팅이 길었다.");
        assertThat(document.getMetadata())
                .containsEntry("documentType", "REVIEW_CHUNK")
                .containsEntry("source", "COMMUNITY_POST")
                .containsEntry("shopId", "1")
                .containsEntry("shopName", "멘야하나비")
                .containsEntry("region", "서울 마포구");
    }
}