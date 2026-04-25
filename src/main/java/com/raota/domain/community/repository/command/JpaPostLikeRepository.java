package com.raota.domain.community.repository.command;

import com.raota.domain.community.repository.command.entity.PostLikeEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

interface JpaPostLikeEntityRepository extends JpaRepository<PostLikeEntity, Long> {
    Optional<PostLikeEntity> findByPostIdAndMemberId(Long postId, Long memberId);
    long countByPostId(Long postId);
}

@Repository
@RequiredArgsConstructor
public class JpaPostLikeRepository implements PostLikeRepository {
    private final JpaPostLikeEntityRepository jpaRepository;

    @Override
    public PostLikeEntity save(PostLikeEntity postLike) {
        return jpaRepository.save(postLike);
    }

    @Override
    public void delete(PostLikeEntity postLike) {
        jpaRepository.delete(postLike);
    }

    @Override
    public Optional<PostLikeEntity> findByPostIdAndMemberId(Long postId, Long memberId) {
        return jpaRepository.findByPostIdAndMemberId(postId, memberId);
    }

    @Override
    public long countByPostId(Long postId) {
        return jpaRepository.countByPostId(postId);
    }
}
