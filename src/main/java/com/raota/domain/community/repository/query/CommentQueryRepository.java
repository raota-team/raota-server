package com.raota.domain.community.repository.query;

import com.raota.domain.community.presentation.response.CommunityCommentItemResponse;
import com.raota.global.common.PageResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class CommentQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<CommunityCommentItemResponse> getComment(Long commentId) {
        List<CommentRow> rows = entityManager.createQuery(
                        """
                        select new com.raota.domain.community.repository.query.CommentQueryRepository$CommentRow(
                               c.id, c.parent.id, c.post.id, m.nickname, m.id, m.imageUrl, pm.nickname,
                               c.content, c.createdAt, c.isDeleted
                        )
                        from CommentEntity c
                        join c.member m
                        left join c.parent pc
                        left join pc.member pm
                        where c.id = :commentId
                        """,
                        CommentRow.class
                )
                .setParameter("commentId", commentId)
                .getResultList();

        return rows.stream().findFirst().map(CommentRow::toResponse);
    }

    public PageResponse<CommunityCommentItemResponse> getParentComments(Long postId, Pageable pageable) {
        Long totalCount = entityManager.createQuery(
                        """
                        select count(c)
                        from CommentEntity c
                        where c.post.id = :postId and c.parent is null
                        """,
                        Long.class
                )
                .setParameter("postId", postId)
                .getSingleResult();

        List<CommentRow> rows = entityManager.createQuery(
                        """
                        select new com.raota.domain.community.repository.query.CommentQueryRepository$CommentRow(
                               c.id, null, c.post.id, m.nickname, m.id, m.imageUrl, null,
                               c.content, c.createdAt, c.isDeleted
                        )
                        from CommentEntity c
                        join c.member m
                        where c.post.id = :postId and c.parent is null
                        order by c.createdAt asc
                        """,
                        CommentRow.class
                )
                .setParameter("postId", postId)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        List<CommunityCommentItemResponse> items = rows.stream()
                .map(CommentRow::toResponse)
                .toList();

        return PageResponse.from(new PageImpl<>(items, pageable, totalCount));
    }

    public List<CommunityCommentItemResponse> getReplies(Long parentId) {
        List<CommentRow> rows = entityManager.createQuery(
                        """
                        select new com.raota.domain.community.repository.query.CommentQueryRepository$CommentRow(
                               c.id, c.parent.id, c.post.id, m.nickname, m.id, m.imageUrl, pm.nickname,
                               c.content, c.createdAt, c.isDeleted
                        )
                        from CommentEntity c
                        join c.member m
                        left join c.parent pc
                        left join pc.member pm
                        where c.parent.id = :parentId
                        order by c.createdAt asc
                        """,
                        CommentRow.class
                )
                .setParameter("parentId", parentId)
                .getResultList();

        return rows.stream()
                .map(CommentRow::toResponse)
                .toList();
    }

    private record CommentRow(
            Long commentId,
            Long parentCommentId,
            Long postId,
            String authorNickname,
            Long authorId,
            String authorImageUrl,
            String taggedParentAuthorNickname,
            String content,
            LocalDateTime createdAt,
            Boolean isDeleted
    ) {
        private CommunityCommentItemResponse toResponse() {
            return new CommunityCommentItemResponse(
                    commentId,
                    parentCommentId,
                    postId,
                    authorNickname,
                    authorId,
                    authorImageUrl,
                    taggedParentAuthorNickname,
                    createdAt,
                    Boolean.TRUE.equals(isDeleted) ? "삭제된 댓글입니다." : content,
                    isDeleted
            );
        }
    }
}
