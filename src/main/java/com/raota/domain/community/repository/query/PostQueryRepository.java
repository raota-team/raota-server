package com.raota.domain.community.repository.query;

import static org.jooq.impl.DSL.*;

import com.raota.domain.community.presentation.request.CommunityPostSearchRequest;
import com.raota.domain.community.presentation.request.CommunityRamenShopSearchRequest;
import com.raota.domain.community.presentation.response.CommunityPostCardResponse;
import com.raota.domain.community.presentation.response.CommunityPostDetailResponse;
import com.raota.domain.community.presentation.response.CommunityRamenShopOptionResponse;
import com.raota.global.common.PageResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PostQueryRepository {
    private final DSLContext dsl;

    public PageResponse<CommunityPostCardResponse> searchPostCards(CommunityPostSearchRequest request, Pageable pageable) {
        log.info("Searching posts with category filter: {}", request.getCategory());

        // 1. 조건 설정 (카테고리 필터 등)
        Condition condition = field("tb_post.is_deleted").eq(false);
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            condition = condition.and(field("tb_post.category").eq(request.getCategory()));
        }

        // 2. 전체 개수 조회
        int totalCount = dsl.fetchCount(
                dsl.selectFrom(table("tb_post")).where(condition)
        );
        List<CommunityPostCardResponse> items = dsl.select(
                        field("tb_post.id").as("postId"),
                        field("tb_post.category"),
                        field("tb_ramen_shop.name").as("storeName"),
                        field("tb_post.title"),
                        substring(field("tb_post.content", String.class), 1, 100).as("contentPreview"),
                        field("tb_post.thumbnail_url").as("imageUrl"),
                        field("tb_member_profile.nickname").as("authorName"),
                        field("tb_post.created_at").as("createdAt"),
                        // 좋아요 수 서브쿼리
                        field(selectCount().from(table("tb_post_like")).where(field("tb_post_like.post_id").eq(field("tb_post.id")))).as("likeCount"),
                        // 댓글 수 서브쿼리 (삭제되지 않은 댓글만)
                        field(selectCount().from(table("tb_comment")).where(field("tb_comment.post_id").eq(field("tb_post.id")).and(field("tb_comment.is_deleted").eq(false)))).as("commentCount")
                )
                .from(table("tb_post"))
                .leftJoin(table("tb_ramen_shop")).on(field("tb_post.ramen_shop_id").eq(field("tb_ramen_shop.ramen_shop_id")))
                .join(table("tb_member_profile")).on(field("tb_post.author_id").eq(field("tb_member_profile.id")))
                .where(condition)
                .orderBy(field("tb_post.created_at").desc())
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetchInto(CommunityPostCardResponse.class);

        Page<CommunityPostCardResponse> page = new PageImpl<>(items, pageable, totalCount);
        return PageResponse.from(page);
    }

    public CommunityPostDetailResponse getPostDetail(Long postId, Long memberId) {
        return dsl.select(
                        field("tb_post.category", String.class).as("category"),
                        field("tb_ramen_shop.name", String.class).as("storeName"),
                        field("tb_post.title", String.class).as("title"),
                        field("tb_member_profile.nickname", String.class).as("authorName"),
                        field("tb_post.created_at", LocalDateTime.class).as("createdAt"),
                        field("tb_post.content_format", String.class).as("contentFormat"),
                        field("tb_post.content", String.class).as("content"),
                        // 좋아요 수 서브쿼리
                        field(selectCount().from(table("tb_post_like")).where(field("tb_post_like.post_id").eq(field("tb_post.id")))).as("likeCount"),
                        // 댓글 수 서브쿼리
                        field(selectCount().from(table("tb_comment")).where(field("tb_comment.post_id").eq(field("tb_post.id")).and(field("tb_comment.is_deleted").eq(false)))).as("commentCount"),
                        // 좋아요 여부 서브쿼리
                        field(memberId == null ? inline(false) : exists(
                                selectOne().from(table("tb_post_like"))
                                        .where(field("tb_post_like.post_id").eq(field("tb_post.id"))
                                                .and(field("tb_post_like.member_id").eq(memberId)))
                        )).as("isLiked")
                )
                .from(table("tb_post"))
                .leftJoin(table("tb_ramen_shop")).on(field("tb_post.ramen_shop_id").eq(field("tb_ramen_shop.ramen_shop_id")))
                .join(table("tb_member_profile")).on(field("tb_post.author_id").eq(field("tb_member_profile.id")))
                .where(field("tb_post.id").eq(postId).and(field("tb_post.is_deleted").eq(false)))
                .fetchOne(r -> new CommunityPostDetailResponse(
                        r.get("category", String.class),
                        r.get("storeName", String.class),
                        r.get("title", String.class),
                        r.get("authorName", String.class),
                        r.get("createdAt", LocalDateTime.class),
                        java.util.Collections.emptyList(), // 이미지 리스트는 본문에 포함됨
                        r.get("contentFormat", String.class),
                        r.get("content", String.class),
                        r.get("likeCount", Long.class),
                        r.get("commentCount", Long.class),
                        r.get("isLiked", Boolean.class)
                ));
    }

    public PageResponse<CommunityRamenShopOptionResponse> getRamenShopOptions(CommunityRamenShopSearchRequest request, Pageable pageable) {
        Condition condition = trueCondition();
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            condition = condition.and(
                    field("tb_ramen_shop.name").like("%" + request.getKeyword() + "%")
                    .or(field("tb_ramen_shop.city").like("%" + request.getKeyword() + "%"))
                    .or(field("tb_ramen_shop.district").like("%" + request.getKeyword() + "%"))
            );
        }

        int totalCount = dsl.fetchCount(selectFrom(table("tb_ramen_shop")).where(condition));

        List<CommunityRamenShopOptionResponse> items = dsl.select(
                        field("tb_ramen_shop.ramen_shop_id", Long.class).as("id"),
                        field("tb_ramen_shop.name", String.class).as("name"),
                        concat(field("tb_ramen_shop.city", String.class), inline(" "), field("tb_ramen_shop.district", String.class)).as("region"),
                        field("tb_ramen_shop.image_url", String.class).as("thumbnailUrl")
                )
                .from(table("tb_ramen_shop"))
                .where(condition)
                .orderBy(field("tb_ramen_shop.name").asc())
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetch(r -> new CommunityRamenShopOptionResponse(
                        r.get("id", Long.class),
                        r.get("name", String.class),
                        r.get("region", String.class),
                        r.get("thumbnailUrl", String.class)
                ));

        return PageResponse.from(new PageImpl<>(items, pageable, totalCount));
    }
}
