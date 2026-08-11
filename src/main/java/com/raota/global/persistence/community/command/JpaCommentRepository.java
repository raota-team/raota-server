package com.raota.global.persistence.community.command;

import com.raota.domain.community.model.Comment;
import com.raota.domain.community.repository.CommentRepository;
import com.raota.global.persistence.community.entity.CommentEntity;
import com.raota.global.persistence.community.entity.PostEntity;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

interface JpaCommentEntityRepository extends JpaRepository<CommentEntity, Long> {
    List<CommentEntity> findAllByPostId(Long postId);
}

@Repository
@RequiredArgsConstructor
public class JpaCommentRepository implements CommentRepository {
    private final JpaCommentEntityRepository jpaRepository;
    private final JpaPostEntityRepository postJpaRepository;
    private final MemberRepository memberRepository;

    @Override
    public Comment save(Comment comment) {
        PostEntity post = postJpaRepository.findById(comment.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        MemberProfile author = memberRepository.findById(comment.getAuthorId())
                .orElseThrow(() -> new IllegalArgumentException("작성자를 찾을 수 없습니다."));
        
        CommentEntity parent = null;
        if (comment.getParentId() != null) {
            parent = jpaRepository.findById(comment.getParentId()).orElse(null);
        }

        CommentEntity entity = CommentEntity.fromDomain(comment, post, author, parent);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Comment> findById(Long id) {
        return jpaRepository.findById(id).map(CommentEntity::toDomain);
    }

    @Override
    public void validateReplyTarget(Long parentId) {
        if (parentId == null) {
            return;
        }

        CommentEntity parent = findCommentEntity(parentId, "부모 댓글이 존재하지 않습니다.");

        if (parent.getParent() != null) {
            throw new IllegalArgumentException("답글에는 답글을 달 수 없습니다. (최대 Depth 1)");
        }
    }

    @Override
    public void update(Long id, Long authorId, String content) {
        CommentEntity commentEntity = findCommentEntity(id, "댓글을 찾을 수 없습니다.");

        if (!commentEntity.getMember().getId().equals(authorId)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        commentEntity.update(content);
    }

    @Override
    public void softDelete(Long id, Long authorId) {
        CommentEntity commentEntity = findCommentEntity(id, "댓글을 찾을 수 없습니다.");

        if (!commentEntity.getMember().getId().equals(authorId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        commentEntity.delete();
    }

    @Override
    public List<Comment> findAllByPostId(Long postId) {
        return jpaRepository.findAllByPostId(postId).stream()
                .map(CommentEntity::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    public void deleteAll() {
        jpaRepository.deleteAll();
    }

    public void flush() {
        jpaRepository.flush();
    }

    private CommentEntity findCommentEntity(Long id, String message) {
        return jpaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(message));
    }
}
