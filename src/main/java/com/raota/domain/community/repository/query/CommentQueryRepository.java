package com.raota.domain.community.repository.query;

import static org.jooq.impl.DSL.*;

import com.raota.domain.community.presentation.response.CommunityCommentItemResponse;
import com.raota.global.common.PageResponse;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentQueryRepository {
    private final DSLContext dsl;

    public Optional<CommunityCommentItemResponse> getComment(Long commentId) {
        Table<?> parentComment = table("tb_comment").as("parent_comment");
        Table<?> parentMember = table("tb_member_profile").as("parent_member");

        return dsl.select(
                        field("tb_comment.id", Long.class).as("commentId"),
                        field("tb_comment.parent_id", Long.class).as("parentCommentId"),
                        field("tb_comment.post_id", Long.class).as("postId"),
                        field("tb_member_profile.nickname", String.class).as("authorNickname"),
                        field("parent_member.nickname", String.class).as("taggedParentNickname"),
                        field("tb_comment.content", String.class).as("content"),
                        field("tb_comment.created_at", java.time.LocalDateTime.class).as("createdAt"),
                        field("tb_comment.is_deleted", Boolean.class).as("isDeleted")
                )
                .from(table("tb_comment"))
                .join(table("tb_member_profile")).on(field("tb_comment.member_id").eq(field("tb_member_profile.id")))
                .leftJoin(parentComment).on(field("tb_comment.parent_id").eq(field("parent_comment.id")))
                .leftJoin(parentMember).on(field("parent_comment.member_id").eq(field("parent_member.id")))
                .where(field("tb_comment.id").eq(commentId))
                .fetchOptional(r -> {
                    boolean isDeleted = r.get("isDeleted", Boolean.class);
                    return new CommunityCommentItemResponse(
                        r.get("commentId", Long.class),
                        r.get("parentCommentId", Long.class),
                        r.get("postId", Long.class),
                        r.get("authorNickname", String.class),
                        r.get("taggedParentNickname", String.class),
                        r.get("createdAt", java.time.LocalDateTime.class),
                        isDeleted ? "삭제된 댓글입니다." : r.get("content", String.class),
                        isDeleted
                    );
                });
    }

    /**
     * 부모 댓글(Parent)만 페이지네이션하여 조회한다.
     * 삭제된 댓글도 포함하여 조회한다.
     */
    public PageResponse<CommunityCommentItemResponse> getParentComments(Long postId, Pageable pageable) {
        int totalCount = dsl.fetchCount(
                selectFrom(table("tb_comment"))
                        .where(field("tb_comment.post_id").eq(postId)
                                .and(field("tb_comment.parent_id").isNull()))
        );

        List<CommunityCommentItemResponse> items = dsl.select(
                        field("tb_comment.id", Long.class).as("commentId"),
                        field("tb_comment.post_id", Long.class).as("postId"),
                        field("tb_member_profile.nickname", String.class).as("authorNickname"),
                        field("tb_comment.created_at", java.time.LocalDateTime.class).as("createdAt"),
                        field("tb_comment.content", String.class).as("content"),
                        field("tb_comment.is_deleted", Boolean.class).as("isDeleted")
                )
                .from(table("tb_comment"))
                .join(table("tb_member_profile")).on(field("tb_comment.member_id").eq(field("tb_member_profile.id")))
                .where(field("tb_comment.post_id").eq(postId)
                        .and(field("tb_comment.parent_id").isNull()))
                .orderBy(field("tb_comment.created_at").asc())
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetch(r -> {
                    boolean isDeleted = r.get("isDeleted", Boolean.class);
                    return new CommunityCommentItemResponse(
                        r.get("commentId", Long.class),
                        null,
                        r.get("postId", Long.class),
                        r.get("authorNickname", String.class),
                        null,
                        r.get("createdAt", java.time.LocalDateTime.class),
                        isDeleted ? "삭제된 댓글입니다." : r.get("content", String.class),
                        isDeleted
                    );
                });

        return PageResponse.from(new PageImpl<>(items, pageable, totalCount));
    }

    /**
     * 특정 부모 댓글의 답글(Replies)을 조회한다.
     * 삭제된 답글도 포함한다.
     */
    public List<CommunityCommentItemResponse> getReplies(Long parentId) {
        Table<?> parentComment = table("tb_comment").as("parent_comment");
        Table<?> parentMember = table("tb_member_profile").as("parent_member");

        return dsl.select(
                        field("tb_comment.id", Long.class).as("commentId"),
                        field("tb_comment.parent_id", Long.class).as("parentCommentId"),
                        field("tb_comment.post_id", Long.class).as("postId"),
                        field("tb_member_profile.nickname", String.class).as("authorNickname"),
                        field("parent_member.nickname", String.class).as("taggedParentNickname"),
                        field("tb_comment.created_at", java.time.LocalDateTime.class).as("createdAt"),
                        field("tb_comment.content", String.class).as("content"),
                        field("tb_comment.is_deleted", Boolean.class).as("isDeleted")
                )
                .from(table("tb_comment"))
                .join(table("tb_member_profile")).on(field("tb_comment.member_id").eq(field("tb_member_profile.id")))
                .leftJoin(parentComment).on(field("tb_comment.parent_id").eq(field("parent_comment.id")))
                .leftJoin(parentMember).on(field("parent_comment.member_id").eq(field("parent_member.id")))
                .where(field("tb_comment.parent_id").eq(parentId))
                .orderBy(field("tb_comment.created_at").asc())
                .fetch(r -> {
                    boolean isDeleted = r.get("isDeleted", Boolean.class);
                    return new CommunityCommentItemResponse(
                        r.get("commentId", Long.class),
                        r.get("parentCommentId", Long.class),
                        r.get("postId", Long.class),
                        r.get("authorNickname", String.class),
                        r.get("taggedParentNickname", String.class),
                        r.get("createdAt", java.time.LocalDateTime.class),
                        isDeleted ? "삭제된 댓글입니다." : r.get("content", String.class),
                        isDeleted
                    );
                });
    }
}
