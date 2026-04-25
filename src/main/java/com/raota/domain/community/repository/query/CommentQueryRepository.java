package com.raota.domain.community.repository.query;

import static org.jooq.impl.DSL.*;

import com.raota.domain.community.presentation.response.CommunityCommentItemResponse;
import com.raota.global.common.PageResponse;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentQueryRepository {
    private final DSLContext dsl;

    public Optional<CommunityCommentItemResponse> getComment(Long commentId) {
        return dsl.select(
                        field("tb_comment.id", Long.class).as("commentId"),
                        field("tb_comment.parent_id", Long.class).as("parentCommentId"),
                        field("tb_member_profile.nickname", String.class).as("authorNickname"),
                        field("tb_comment.content", String.class).as("content"),
                        field("tb_comment.created_at", java.time.LocalDateTime.class).as("createdAt")
                )
                .from(table("tb_comment"))
                .join(table("tb_member_profile")).on(field("tb_comment.member_id").eq(field("tb_member_profile.id")))
                .where(field("tb_comment.id").eq(commentId))
                .fetchOptional(r -> new CommunityCommentItemResponse(
                        r.get("commentId", Long.class),
                        r.get("parentCommentId", Long.class),
                        r.get("authorNickname", String.class),
                        null, // taggedParentAuthorNickname (기능 미구현 대응)
                        r.get("createdAt", java.time.LocalDateTime.class),
                        r.get("content", String.class)
                ));
    }

    public PageResponse<CommunityCommentItemResponse> getComments(Long postId, Pageable pageable) {
        int totalCount = dsl.fetchCount(
                selectFrom(table("tb_comment")).where(field("tb_comment.post_id").eq(postId))
        );

        List<CommunityCommentItemResponse> items = dsl.select(
                        field("tb_comment.id", Long.class).as("commentId"),
                        field("tb_comment.parent_id", Long.class).as("parentCommentId"),
                        field("tb_member_profile.nickname", String.class).as("authorNickname"),
                        field("tb_comment.created_at", java.time.LocalDateTime.class).as("createdAt"),
                        field("tb_comment.content", String.class).as("content")
                )
                .from(table("tb_comment"))
                .join(table("tb_member_profile")).on(field("tb_comment.member_id").eq(field("tb_member_profile.id")))
                .where(field("tb_comment.post_id").eq(postId))
                .orderBy(field("tb_comment.created_at").asc())
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetch(r -> new CommunityCommentItemResponse(
                        r.get("commentId", Long.class),
                        r.get("parentCommentId", Long.class),
                        r.get("authorNickname", String.class),
                        null, // taggedParentAuthorNickname (미구현)
                        r.get("createdAt", java.time.LocalDateTime.class),
                        r.get("content", String.class)
                ));

        return PageResponse.from(new PageImpl<>(items, pageable, totalCount));
    }
}
