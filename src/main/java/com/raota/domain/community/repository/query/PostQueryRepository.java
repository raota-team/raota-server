package com.raota.domain.community.repository.query;

import com.raota.domain.community.model.PostCategory;
import com.raota.presentation.api.community.request.CommunityPostSearchRequest;
import com.raota.presentation.api.community.request.CommunityRamenShopSearchRequest;
import com.raota.presentation.api.community.response.CommunityPostCardResponse;
import com.raota.presentation.api.community.response.CommunityPostDetailResponse;
import com.raota.presentation.api.community.response.CommunityRamenShopOptionResponse;
import com.raota.presentation.common.PageResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class PostQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public PageResponse<CommunityPostCardResponse> searchPostCards(CommunityPostSearchRequest request, Pageable pageable) {
        log.info("Searching posts with category filter: {}, ramenShopId filter: {}",
                request.getCategory(), request.getRamenShopId());

        PostCategory category = parseCategory(request.getCategory());

        String whereClause = """
                where p.isDeleted = false
                  and (:category is null or p.category = :category)
                  and (:ramenShopId is null or rs.id = :ramenShopId)
                """;

        Long totalCount = entityManager.createQuery(
                        "select count(p) from PostEntity p left join p.ramenShop rs " + whereClause,
                        Long.class
                )
                .setParameter("category", category)
                .setParameter("ramenShopId", request.getRamenShopId())
                .getSingleResult();

        TypedQuery<PostCardRow> query = entityManager.createQuery(
                        """
                        select new com.raota.domain.community.repository.query.PostQueryRepository$PostCardRow(
                               p.id, p.category, rs.id, rs.name, p.title, substring(p.content, 1, 100),
                               p.thumbnailUrl, a.nickname, a.id, a.imageUrl, p.createdAt,
                               (select count(pl) from PostLikeEntity pl where pl.postId = p.id),
                               (select count(c) from CommentEntity c where c.post.id = p.id and c.isDeleted = false)
                        )
                        from PostEntity p
                        left join p.ramenShop rs
                        join p.author a
                        """ + whereClause + """
                        order by p.createdAt desc
                        """,
                        PostCardRow.class
                );

        List<PostCardRow> rows = query
                .setParameter("category", category)
                .setParameter("ramenShopId", request.getRamenShopId())
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        List<CommunityPostCardResponse> items = rows.stream()
                .map(PostCardRow::toResponse)
                .toList();

        return PageResponse.from(new PageImpl<>(items, pageable, totalCount));
    }

    public CommunityPostDetailResponse getPostDetail(Long postId, Long memberId) {
        List<PostDetailRow> rows = entityManager.createQuery(
                        """
                        select new com.raota.domain.community.repository.query.PostQueryRepository$PostDetailRow(
                               p.category, rs.name, p.title, a.nickname, a.id, a.imageUrl, p.createdAt,
                               p.contentFormat, p.content,
                               (select count(pl) from PostLikeEntity pl where pl.postId = p.id),
                               (select count(c) from CommentEntity c where c.post.id = p.id and c.isDeleted = false),
                               (select count(pl2) from PostLikeEntity pl2 where pl2.postId = p.id and pl2.memberId = :memberId)
                        )
                        from PostEntity p
                        left join p.ramenShop rs
                        join p.author a
                        where p.id = :postId and p.isDeleted = false
                        """,
                        PostDetailRow.class
                )
                .setParameter("postId", postId)
                .setParameter("memberId", memberId == null ? -1L : memberId)
                .getResultList();

        if (rows.isEmpty()) {
            return null;
        }

        return rows.getFirst().toResponse(memberId != null);
    }

    public PageResponse<CommunityRamenShopOptionResponse> getRamenShopOptions(CommunityRamenShopSearchRequest request, Pageable pageable) {
        String normalizedKeyword = normalizeKeyword(request.getKeyword());

        Long totalCount = entityManager.createQuery(
                        """
                        select count(rs)
                        from RamenShop rs
                        where (:keyword is null
                               or rs.name like :keyword
                               or rs.address.city like :keyword
                               or rs.address.district like :keyword)
                        """,
                        Long.class
                )
                .setParameter("keyword", normalizedKeyword)
                .getSingleResult();

        List<RamenShopOptionRow> rows = entityManager.createQuery(
                        """
                        select new com.raota.domain.community.repository.query.PostQueryRepository$RamenShopOptionRow(
                               rs.id, rs.name, concat(rs.address.city, ' ', rs.address.district), rs.imageUrl
                        )
                        from RamenShop rs
                        where (:keyword is null
                               or rs.name like :keyword
                               or rs.address.city like :keyword
                               or rs.address.district like :keyword)
                        order by rs.name asc
                        """,
                        RamenShopOptionRow.class
                )
                .setParameter("keyword", normalizedKeyword)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        List<CommunityRamenShopOptionResponse> items = rows.stream()
                .map(RamenShopOptionRow::toResponse)
                .toList();

        return PageResponse.from(new PageImpl<>(items, pageable, totalCount));
    }

    private PostCategory parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return PostCategory.valueOf(category);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword + "%";
    }

    private record PostCardRow(
            Long postId,
            PostCategory category,
            Long ramenShopId,
            String storeName,
            String title,
            String contentPreview,
            String imageUrl,
            String authorName,
            Long authorId,
            String authorImageUrl,
            LocalDateTime createdAt,
            Long likeCount,
            Long commentCount
    ) {
        private CommunityPostCardResponse toResponse() {
            return new CommunityPostCardResponse(
                    postId,
                    category.name(),
                    ramenShopId,
                    storeName,
                    title,
                    contentPreview,
                    imageUrl,
                    authorName,
                    authorId,
                    authorImageUrl,
                    createdAt,
                    likeCount,
                    commentCount
            );
        }
    }

    private record PostDetailRow(
            PostCategory category,
            String storeName,
            String title,
            String authorName,
            Long authorId,
            String authorImageUrl,
            LocalDateTime createdAt,
            String contentFormat,
            String content,
            Long likeCount,
            Long commentCount,
            Long likedCount
    ) {
        private CommunityPostDetailResponse toResponse(boolean hasMemberContext) {
            return new CommunityPostDetailResponse(
                    category.name(),
                    storeName,
                    title,
                    authorName,
                    authorId,
                    authorImageUrl,
                    createdAt,
                    Collections.emptyList(),
                    contentFormat,
                    content,
                    likeCount,
                    commentCount,
                    hasMemberContext && likedCount > 0
            );
        }
    }

    private record RamenShopOptionRow(
            Long id,
            String name,
            String region,
            String thumbnailUrl
    ) {
        private CommunityRamenShopOptionResponse toResponse() {
            return new CommunityRamenShopOptionResponse(id, name, region, thumbnailUrl);
        }
    }
}
