package com.raota.domain.community.repository.query;

import static org.jooq.impl.DSL.*;

import com.raota.domain.community.presentation.request.CommunityPostSearchRequest;
import com.raota.domain.community.presentation.request.CommunityRamenShopSearchRequest;
import com.raota.domain.community.presentation.response.CommunityPostCardResponse;
import com.raota.domain.community.presentation.response.CommunityPostDetailResponse;
import com.raota.domain.community.presentation.response.CommunityRamenShopOptionResponse;
import com.raota.global.common.PageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostQueryRepository {
    private final DSLContext dsl;

    public PageResponse<CommunityPostCardResponse> searchPostCards(CommunityPostSearchRequest request, Pageable pageable) {
        // TODO: JOOQ DSL을 사용한 페이징 조회 구현
        return PageResponse.from(org.springframework.data.domain.Page.empty(pageable));
    }

    public CommunityPostDetailResponse getPostDetail(Long postId) {
        // TODO: JOOQ DSL을 사용한 단일 게시글 상세 조회 구현
        return null;
    }

    public PageResponse<CommunityRamenShopOptionResponse> getRamenShopOptions(CommunityRamenShopSearchRequest request, Pageable pageable) {
        // TODO: JOOQ DSL을 사용한 라멘집 옵션 목록 조회 구현
        return PageResponse.from(org.springframework.data.domain.Page.empty(pageable));
    }
}
