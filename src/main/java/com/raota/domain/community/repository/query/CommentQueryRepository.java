package com.raota.domain.community.repository.query;

import com.raota.domain.community.presentation.response.CommunityCommentItemResponse;
import com.raota.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentQueryRepository {
    private final DSLContext dsl;

    public PageResponse<CommunityCommentItemResponse> getComments(Long postId, Pageable pageable) {
        // TODO: JOOQ DSL을 사용한 댓글 목록 페이징 조회 구현
        return PageResponse.from(org.springframework.data.domain.Page.empty(pageable));
    }
}
