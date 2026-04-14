package com.raota.domain.community.model;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Post {
    private final Long id;
    private final PostCategory category;
    private final String title;
    private final String content;
    private final String contentFormat;
    private final String thumbnailUrl;
    private final Long authorId;
    private final Long ramenShopId;
    private final LocalDateTime createdAt;

    /**
     * 새로운 커뮤니티 게시글을 생성한다.
     */
    public static Post create(
            PostCategory category,
            String title,
            String content,
            String contentFormat,
            String thumbnailUrl,
            Long authorId,
            Long ramenShopId
    ) {
        validate(title, content, authorId);
        return Post.builder()
                .category(category)
                .title(title)
                .content(content)
                .contentFormat(contentFormat)
                .thumbnailUrl(thumbnailUrl)
                .authorId(authorId)
                .ramenShopId(ramenShopId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 기존에 저장된 게시글 데이터를 도메인 모델로 로드한다.
     */
    public static Post of(
            Long id,
            PostCategory category,
            String title,
            String content,
            String contentFormat,
            String thumbnailUrl,
            Long authorId,
            Long ramenShopId,
            LocalDateTime createdAt
    ) {
        return Post.builder()
                .id(id)
                .category(category)
                .title(title)
                .content(content)
                .contentFormat(contentFormat)
                .thumbnailUrl(thumbnailUrl)
                .authorId(authorId)
                .ramenShopId(ramenShopId)
                .createdAt(createdAt)
                .build();
    }

    private static void validate(String title, String content, Long authorId) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("게시글 제목은 필수입니다.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("게시글 본문은 필수입니다.");
        }
        if (authorId == null) {
            throw new IllegalArgumentException("작성자 정보는 필수입니다.");
        }
    }

    /**
     * 게시글의 내용을 수정한다.
     */
    public Post update(String title, String content, String thumbnailUrl, Long ramenShopId) {
        validate(title, content, this.authorId);
        return Post.builder()
                .id(this.id)
                .category(this.category)
                .title(title)
                .content(content)
                .contentFormat(this.contentFormat)
                .thumbnailUrl(thumbnailUrl)
                .authorId(this.authorId)
                .ramenShopId(ramenShopId)
                .createdAt(this.createdAt)
                .build();
    }
}
