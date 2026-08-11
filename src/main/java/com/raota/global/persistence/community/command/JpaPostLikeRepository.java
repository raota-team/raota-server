package com.raota.global.persistence.community.command;

import com.raota.domain.community.repository.PostLikeRepository;
import com.raota.global.persistence.community.entity.PostLikeEntity;
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
    public boolean toggle(Long postId, Long memberId) {
        Optional<PostLikeEntity> existingLike = jpaRepository.findByPostIdAndMemberId(postId, memberId);

        if (existingLike.isPresent()) {
            jpaRepository.delete(existingLike.get());
            return false;
        }

        jpaRepository.save(PostLikeEntity.builder()
                .postId(postId)
                .memberId(memberId)
                .build());
        return true;
    }

    @Override
    public long countByPostId(Long postId) {
        return jpaRepository.countByPostId(postId);
    }
}
