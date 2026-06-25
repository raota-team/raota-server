package com.raota.infrastructure.persistence.community.query;

import com.raota.application.community.query.PostSearchQuery;
import com.raota.application.community.port.PostQueryPort;
import com.raota.application.community.result.HomePostResult;
import com.raota.application.community.result.PopularPostResult;
import com.raota.application.community.result.PostCardResult;
import com.raota.application.community.result.PostDetailResult;
import com.raota.application.community.result.RamenShopOptionResult;
import com.raota.domain.community.model.PostCategory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class PostQueryRepository implements PostQueryPort {

    private static final long POPULAR_POST_MIN_LIKE_COUNT = 3L;

    @PersistenceContext
    private EntityManager entityManager;

    public Page<PostCardResult> searchPostCards(PostSearchQuery request, Pageable pageable) {
        log.info("Searching posts with category filter: {}, ramenShopId filter: {}",
                request.category(), request.ramenShopId());

        boolean popularOnly = isPopularCategory(request.category());
        PostCategory category = popularOnly ? null : parseCategory(request.category());

        String whereClause = """
                where p.isDeleted = false
                  and (:category is null or p.category = :category)
                  and (:ramenShopId is null or rs.id = :ramenShopId)
                  and (:popularOnly = false or
                       (select count(popularLike) from PostLikeEntity popularLike where popularLike.postId = p.id)
                       >= :popularMinLikeCount)
                """;

        Long totalCount = entityManager.createQuery(
                        "select count(p) from PostEntity p left join p.ramenShop rs " + whereClause,
                        Long.class
                )
                .setParameter("category", category)
                .setParameter("ramenShopId", request.ramenShopId())
                .setParameter("popularOnly", popularOnly)
                .setParameter("popularMinLikeCount", POPULAR_POST_MIN_LIKE_COUNT)
                .getSingleResult();

        TypedQuery<PostCardRow> query = entityManager.createQuery(
                        """
                        select new com.raota.infrastructure.persistence.community.query.PostQueryRepository$PostCardRow(
                               p.id, p.category, rs.id, rs.name, p.title, substring(p.content, 1, 100),
                               p.thumbnailUrl, a.nickname, a.id, a.imageUrl, p.createdAt,
                               (select count(pl) from PostLikeEntity pl where pl.postId = p.id),
                               (select count(c) from CommentEntity c where c.post.id = p.id and c.isDeleted = false),
                               p.viewCount
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
                .setParameter("ramenShopId", request.ramenShopId())
                .setParameter("popularOnly", popularOnly)
                .setParameter("popularMinLikeCount", POPULAR_POST_MIN_LIKE_COUNT)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        List<PostCardResult> items = rows.stream()
                .map(PostCardRow::toResult)
                .toList();

        return new PageImpl<>(items, pageable, totalCount);
    }

    public PostDetailResult getPostDetail(Long postId, Long memberId) {
        List<PostDetailRow> rows = entityManager.createQuery(
                        """
                        select new com.raota.infrastructure.persistence.community.query.PostQueryRepository$PostDetailRow(
                               p.category, rs.name, p.title, a.nickname, a.id, a.imageUrl, p.createdAt,
                               p.contentFormat, p.content,
                               (select count(pl) from PostLikeEntity pl where pl.postId = p.id),
                               (select count(c) from CommentEntity c where c.post.id = p.id and c.isDeleted = false),
                               p.viewCount,
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

        return rows.getFirst().toResult(memberId != null);
    }

    public Page<RamenShopOptionResult> getRamenShopOptions(String keyword, Pageable pageable) {
        String normalizedKeyword = normalizeKeyword(keyword);

        Long totalCount = entityManager.createQuery(
                        """
                        select count(rs)
                        from RamenShop rs
                        where rs.published = true
                          and (:keyword is null
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
                        select new com.raota.infrastructure.persistence.community.query.PostQueryRepository$RamenShopOptionRow(
                               rs.id, rs.name, concat(rs.address.city, ' ', rs.address.district), rs.imageUrl
                        )
                        from RamenShop rs
                        where rs.published = true
                          and (:keyword is null
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

        List<RamenShopOptionResult> items = rows.stream()
                .map(RamenShopOptionRow::toResult)
                .toList();

        return new PageImpl<>(items, pageable, totalCount);
    }

    public List<HomePostResult> findHomePosts(String categoryName, int limit) {
        PostCategory category = parseCategory(categoryName);

        TypedQuery<HomePostRow> query = entityManager.createQuery(
                """
                select new com.raota.infrastructure.persistence.community.query.PostQueryRepository$HomePostRow(
                       p.id, p.title, substring(p.content, 1, 50),
                       a.nickname, a.imageUrl,
                       (select count(c) from CommentEntity c where c.post.id = p.id and c.isDeleted = false),
                       p.viewCount,
                       p.createdAt
                )
                from PostEntity p
                join p.author a
                where p.isDeleted = false
                  and (:category is null or p.category = :category)
                order by p.createdAt desc
                """,
                HomePostRow.class
        );

        return query.setParameter("category", category)
                .setMaxResults(limit)
                .getResultList()
                .stream()
                .map(HomePostRow::toResult)
                .toList();
    }

    public List<PopularPostResult> findRecentPopularPosts(int limit) {
        return entityManager.createQuery(
                        """
                        select new com.raota.infrastructure.persistence.community.query.PostQueryRepository$PopularPostRow(
                               p.id, p.category, p.title,
                               (select count(pl) from PostLikeEntity pl where pl.postId = p.id),
                               (select count(c) from CommentEntity c where c.post.id = p.id and c.isDeleted = false),
                               p.createdAt
                        )
                        from PostEntity p
                        where p.isDeleted = false
                          and (select count(popularLike) from PostLikeEntity popularLike where popularLike.postId = p.id)
                              >= :popularMinLikeCount
                        order by p.createdAt desc
                        """,
                        PopularPostRow.class
                )
                .setParameter("popularMinLikeCount", POPULAR_POST_MIN_LIKE_COUNT)
                .setMaxResults(limit)
                .getResultList()
                .stream()
                .map(PopularPostRow::toResult)
                .toList();
    }

    private PostCategory parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        try {
            return PostCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isPopularCategory(String category) {
        return category != null && "POPULAR".equalsIgnoreCase(category.trim());
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
            Long commentCount,
            Integer viewCount
    ) {
        private PostCardResult toResult() {
            return new PostCardResult(
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
                    commentCount,
                    viewCount
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
            Integer viewCount,
            Long likedCount
    ) {
        private PostDetailResult toResult(boolean hasMemberContext) {
            return new PostDetailResult(
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
                    viewCount,
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
        private RamenShopOptionResult toResult() {
            return new RamenShopOptionResult(id, name, region, thumbnailUrl);
        }
    }

    private record HomePostRow(
            Long id,
            String title,
            String contentSnippet,
            String nickname,
            String profileImageUrl,
            Long commentCount,
            Integer viewCount,
            LocalDateTime createdAt
    ) {
        private HomePostResult toResult() {
            return new HomePostResult(
                    id,
                    title,
                    contentSnippet,
                    new HomePostResult.AuthorSummary(nickname, profileImageUrl),
                    commentCount,
                    viewCount,
                    createdAt
            );
        }
    }

    private record PopularPostRow(
            Long postId,
            PostCategory category,
            String title,
            Long likeCount,
            Long commentCount,
            LocalDateTime createdAt
    ) {
        private PopularPostResult toResult() {
            return new PopularPostResult(
                    postId,
                    category.name(),
                    categoryDisplayName(category),
                    title,
                    likeCount,
                    commentCount,
                    createdAt
            );
        }

        private static String categoryDisplayName(PostCategory category) {
            return switch (category) {
                case REVIEW -> "맛집후기";
                case TIP -> "라멘꿀팁";
                case QUESTION -> "Q&A";
                case FREE -> "자유게시판";
            };
        }
    }
}
