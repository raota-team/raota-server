package com.raota.infrastructure.persistence.community.query;

import com.raota.application.community.port.CommentQueryPort;
import com.raota.application.community.result.CommentItemResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class CommentQueryRepository implements CommentQueryPort {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<CommentItemResult> getComment(Long commentId) {
        List<CommentRow> rows = entityManager.createQuery(
                        """
                        select new com.raota.infrastructure.persistence.community.query.CommentQueryRepository$CommentRow(
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

        return rows.stream().findFirst().map(CommentRow::toResult);
    }

    public Page<CommentItemResult> getParentComments(Long postId, Pageable pageable) {
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
                        select new com.raota.infrastructure.persistence.community.query.CommentQueryRepository$CommentRow(
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

        List<CommentItemResult> items = rows.stream()
                .map(CommentRow::toResult)
                .toList();

        return new PageImpl<>(items, pageable, totalCount);
    }

    @Override
    public Page<CommentItemResult> findCommentsByAuthor(Long authorId, Pageable pageable) {
        Long totalCount = entityManager.createQuery(
                        """
                        select count(c)
                        from CommentEntity c
                        where c.member.id = :authorId
                          and c.member.deletedAt is null
                          and c.isDeleted = false
                          and c.post.isDeleted = false
                        """,
                        Long.class
                )
                .setParameter("authorId", authorId)
                .getSingleResult();

        List<CommentItemResult> items = entityManager.createQuery(
                        """
                        select new com.raota.infrastructure.persistence.community.query.CommentQueryRepository$CommentRow(
                               c.id, c.parent.id, c.post.id, m.nickname, m.id, m.imageUrl, null,
                               c.content, c.createdAt, c.isDeleted
                        )
                        from CommentEntity c
                        join c.member m
                        where m.id = :authorId
                          and m.deletedAt is null
                          and c.isDeleted = false
                          and c.post.isDeleted = false
                        order by c.createdAt desc
                        """,
                        CommentRow.class
                )
                .setParameter("authorId", authorId)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList()
                .stream()
                .map(CommentRow::toResult)
                .toList();

        return new PageImpl<>(items, pageable, totalCount);
    }

    public List<CommentItemResult> getReplies(Long parentId) {
        List<CommentRow> rows = entityManager.createQuery(
                        """
                        select new com.raota.infrastructure.persistence.community.query.CommentQueryRepository$CommentRow(
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
                .map(CommentRow::toResult)
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
        private CommentItemResult toResult() {
            return new CommentItemResult(
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
