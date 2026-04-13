package com.raota.domain.community.repository.query;

import static org.jooq.impl.DSL.*;

import com.raota.domain.community.presentation.response.CommunityCommentItemResponse;
import com.raota.global.common.PageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentQueryRepository {
    private final DSLContext dsl;

    public PageResponse<CommunityCommentItemResponse> getComments(Long postId, Pageable pageable) {
        int totalCount = dsl.fetchCount(
                selectFrom(table("comments")).where(field("comments.post_id").eq(postId))
        );

        List<CommunityCommentItemResponse> items = dsl.select(
                        field("comments.id"),
                        field("member_profile.id").as("authorId"),
                        field("member_profile.nickname").as("authorName"),
                        field("member_profile.image_url").as("authorImageUrl"),
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
