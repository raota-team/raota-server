package com.raota.domain.community.repository.command;

import com.raota.domain.community.model.Comment;
import com.raota.domain.community.repository.command.entity.CommentEntity;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
    private final MemberRepository memberRepository;

    @Override
    public Comment save(Comment comment) {
        MemberProfile author = memberRepository.findById(comment.getAuthorId())
                .orElseThrow(() -> new IllegalArgumentException("작성자를 찾을 수 없습니다."));
        
        CommentEntity entity = CommentEntity.fromDomain(comment, author);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Comment> findById(Long id) {
        return jpaRepository.findById(id).map(CommentEntity::toDomain);
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<Comment> findAllByPostId(Long postId) {
        return jpaRepository.findAllByPostId(postId).stream()
                .map(CommentEntity::toDomain)
                .collect(Collectors.toList());
    }
}
