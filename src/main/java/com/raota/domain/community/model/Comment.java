package com.raota.domain.community.model;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Comment {
    private final Long id;
    private final Long postId;
    private final Long authorId;
    private final Long parentId;
    private final String content;
    private final LocalDateTime createdAt;

    /**
     * 새로운 댓글을 생성한다.
     */
    public static Comment create(Long postId, Long authorId, String content) {
        return create(postId, authorId, null, content);
    }

    public static Comment create(Long postId, Long authorId, Long parentId, String content) {
        validate(postId, authorId, content);
        return Comment.builder()
                .postId(postId)
                .authorId(authorId)
                .parentId(parentId)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 기존에 저장된 댓글 데이터를 도메인 모델로 로드한다.
     */
    public static Comment of(Long id, Long postId, Long authorId, String content, LocalDateTime createdAt) {
        return of(id, postId, authorId, null, content, createdAt);
    }

    public static Comment of(Long id, Long postId, Long authorId, Long parentId, String content, LocalDateTime createdAt) {
        return Comment.builder()
                .id(id)
                .postId(postId)
                .authorId(authorId)
                .parentId(parentId)
                .content(content)
                .createdAt(createdAt)
                .build();
    }

    private static void validate(Long postId, Long authorId, String content) {
        if (postId == null) {
            throw new IllegalArgumentException("게시글 정보는 필수입니다.");
        }
        if (authorId == null) {
            throw new IllegalArgumentException("작성자 정보는 필수입니다.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("댓글 내용은 필수입니다.");
        }
    }

    /**
     * 댓글 내용을 수정한다.
     */
    public Comment update(String content) {
        validate(this.postId, this.authorId, content);
        return Comment.builder()
                .id(this.id)
                .postId(this.postId)
                .authorId(this.authorId)
                .parentId(this.parentId)
                .content(content)
                .createdAt(this.createdAt)
                .build();
    }
}
