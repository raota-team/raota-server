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
                        field("comments.id").as("commentId"),
                        field("comments.parent_id").as("parentCommentId"),
                        field("member_profile.nickname").as("authorNickname"),
                        field("comments.content"),
                        field("comments.created_at").as("createdAt")
                )
                .from(table("comments"))
                .join(table("member_profile")).on(field("comments.member_id").eq(field("member_profile.id")))
                .where(field("comments.id").eq(commentId))
                .fetchOptionalInto(CommunityCommentItemResponse.class);
    }

    public PageResponse<CommunityCommentItemResponse> getComments(Long postId, Pageable pageable) {
        int totalCount = dsl.fetchCount(
                selectFrom(table("comments")).where(field("comments.post_id").eq(postId))
        );

        List<CommunityCommentItemResponse> items = dsl.select(
                        field("comments.id").as("commentId"),
                        field("comments.parent_id").as("parentCommentId"),
                        field("member_profile.nickname").as("authorNickname"),
                        field("comments.content"),
                        field("comments.created_at").as("createdAt")
                )
                .from(table("comments"))
                .join(table("member_profile")).on(field("comments.member_id").eq(field("member_profile.id")))
                .where(field("comments.post_id").eq(postId))
                .orderBy(field("comments.created_at").asc())
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetchInto(CommunityCommentItemResponse.class);

        return PageResponse.from(new PageImpl<>(items, pageable, totalCount));
    }
}
