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
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

// 주의: 아래 코드에서 사용하는 POSTS, MEMBERS, RAMEN_SHOPS 등은 jooqCodegen 실행 후 생성될 클래스입니다.
// 실제 컴파일을 위해서는 `./gradlew jooqCodegen` 실행이 필수입니다.

@Repository
@RequiredArgsConstructor
public class PostQueryRepository {
    private final DSLContext dsl;

    public PageResponse<CommunityPostCardResponse> searchPostCards(CommunityPostSearchRequest request, Pageable pageable) {
        // 1. 조건 설정 (카테고리 필터 등)
        Condition condition = trueCondition();
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            condition = condition.and(field("posts.category").eq(request.getCategory()));
        }

        // 2. 전체 개수 조회
        int totalCount = dsl.fetchCount(
                selectFrom(table("posts")).where(condition)
        );

        // 3. 목록 조회 (JOOQ DSL)
        List<CommunityPostCardResponse> items = dsl.select(
                        field("posts.category"),
                        field("ramen_shops.name").as("storeName"),
                        field("posts.title"),
                        substring(field("posts.content", String.class), 1, 100).as("contentPreview"),
                        field("posts.thumbnail_url").as("imageUrl"),
                        field("member_profile.nickname").as("authorName"),
                        field("posts.created_at").as("createdAt"),
                        inline(0L).as("likeCount"), // 좋아요 기능 추가 시 서브쿼리로 변경 가능
                        inline(0L).as("commentCount") // 댓글 수 서브쿼리 연동 가능
                )
                .from(table("posts"))
                .leftJoin(table("ramen_shops")).on(field("posts.ramen_shop_id").eq(field("ramen_shops.id")))
                .join(table("member_profile")).on(field("posts.member_id").eq(field("member_profile.id")))
                .where(condition)
                .orderBy(field("posts.created_at").desc())
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetchInto(CommunityPostCardResponse.class);

        Page<CommunityPostCardResponse> page = new PageImpl<>(items, pageable, totalCount);
        return PageResponse.from(page);
    }

    public CommunityPostDetailResponse getPostDetail(Long postId) {
        return dsl.select(
                        field("posts.category"),
                        field("ramen_shops.name").as("storeName"),
                        field("posts.title"),
                        field("member_profile.nickname").as("authorName"),
                        field("posts.created_at").as("createdAt"),
                        // imageUrls는 별도의 이미지 테이블이 있다면 조인하거나 리스트로 집계
                        field("posts.content_format").as("contentFormat"),
                        field("posts.content"),
                        inline(0L).as("likeCount"),
                        inline(0L).as("commentCount")
                )
                .from(table("posts"))
                .leftJoin(table("ramen_shops")).on(field("posts.ramen_shop_id").eq(field("ramen_shops.id")))
                .join(table("member_profile")).on(field("posts.member_id").eq(field("member_profile.id")))
                .where(field("posts.id").eq(postId))
                .fetchOneInto(CommunityPostDetailResponse.class);
    }

    public PageResponse<CommunityRamenShopOptionResponse> getRamenShopOptions(CommunityRamenShopSearchRequest request, Pageable pageable) {
        Condition condition = trueCondition();
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            condition = condition.and(field("ramen_shops.name").like("%" + request.getKeyword() + "%"));
        }

        int totalCount = dsl.fetchCount(selectFrom(table("ramen_shops")).where(condition));

        List<CommunityRamenShopOptionResponse> items = dsl.select(
                        field("ramen_shops.id"),
                        field("ramen_shops.name"),
                        concat(field("ramen_shops.city"), inline(" "), field("ramen_shops.district")).as("region"),
                        field("ramen_shops.image_url").as("thumbnailUrl")
                )
                .from(table("ramen_shops"))
                .where(condition)
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetchInto(CommunityRamenShopOptionResponse.class);

        return PageResponse.from(new PageImpl<>(items, pageable, totalCount));
    }
}
