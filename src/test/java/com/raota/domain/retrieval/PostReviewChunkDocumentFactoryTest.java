package com.raota.domain.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.domain.community.model.Post;
import com.raota.domain.community.model.PostCategory;
import com.raota.domain.ramenShop.model.Address;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.retrieval.document.factory.PostReviewChunkDocumentFactory;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class PostReviewChunkDocumentFactoryTest {

    @Test
    void create_should_build_review_chunk_document() {
        PostReviewChunkDocumentFactory factory = new PostReviewChunkDocumentFactory();

        RamenShop shop = RamenShop.builder()
                .id(1L)
                .name("멘야하나비")
                .address(Address.of("서울", "마포구", "양화로", "1"))
                .build();

        Post post = Post.of(
                10L,
                PostCategory.REVIEW,
                "국물이 진했음",
                "차슈가 부드럽고 웨이팅이 길었다.차슈가 부드럽고 웨이팅이 길었다.차슈가 부드럽고 웨이팅이 길었다.",
                "TEXT",
                null,
                100L,
                1L,
                0,
                LocalDateTime.now()
        );

        List<Document> documents = factory.create(post, shop);

        assertThat(documents).isNotEmpty();

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

    @Test
    void create_should_ignore_short_review() {
        PostReviewChunkDocumentFactory factory = new PostReviewChunkDocumentFactory();

        Post post = Post.of(
                10L,
                PostCategory.REVIEW,
                "z",
                "z",
                "TEXT",
                null,
                100L,
                null,
                0,
                LocalDateTime.now()
        );

        List<Document> documents = factory.create(post);

        assertThat(documents).isEmpty();
    }

    @Test
    void create_should_add_chunk_metadata() {
        PostReviewChunkDocumentFactory factory = new PostReviewChunkDocumentFactory();

        Post post = Post.of(
                10L,
                PostCategory.REVIEW,
                "국물이 진하고 면 식감이 좋았던 라멘집 후기",
                "차슈가 부드럽고 웨이팅이 있지만 다시 방문",
                "TEXT",
                null,
                100L,
                null,
                0,
                LocalDateTime.now()
        );

        List<Document> documents = factory.create(post);

        assertThat(documents).isNotEmpty();
        Document document = documents.getFirst();
        assertThat(document.getMetadata())
                .containsEntry("chunkIndex", 0)
                .containsEntry("chunkTotal", documents.size())
                .containsEntry("chunkId", "post:10:chunk:0");
    }

    @Test
    void create_should_split_long_review_into_chunks() {
        PostReviewChunkDocumentFactory factory = new PostReviewChunkDocumentFactory();
        String longContent = "라멘국물굳".repeat(10000);

        Post post = Post.of(
                10L,
                PostCategory.REVIEW,
                "국물이 진하고 면 식감이 좋았던 라멘집 후기",
                longContent,
                "TEXT",
                null,
                100L,
                null,
                0,
                LocalDateTime.now()
        );

        List<Document> documents = factory.create(post);

        assertThat(documents).hasSizeGreaterThan(1);

        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            assertThat(document.getMetadata())
                    .containsEntry("chunkIndex", i)
                    .containsEntry("chunkTotal", documents.size())
                    .containsEntry("chunkId", "post:10:chunk:%d".formatted(i));
        }
    }
}
