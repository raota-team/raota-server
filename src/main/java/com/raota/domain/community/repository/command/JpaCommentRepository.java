package com.raota.domain.community.repository.command;

import com.raota.domain.community.model.Comment;
import com.raota.domain.community.repository.command.entity.CommentEntity;
import com.raota.domain.community.repository.command.entity.PostEntity;
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
    public List<Comment> findAllByPostId(Long postId) {
        return jpaRepository.findAllByPostId(postId).stream()
                .map(CommentEntity::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
