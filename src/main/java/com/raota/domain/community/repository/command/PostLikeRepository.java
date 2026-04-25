package com.raota.domain.community.repository.command;

import com.raota.domain.community.repository.command.entity.PostLikeEntity;
import java.util.Optional;

public interface PostLikeRepository {
    PostLikeEntity save(PostLikeEntity postLike);
    void delete(PostLikeEntity postLike);
    Optional<PostLikeEntity> findByPostIdAndMemberId(Long postId, Long memberId);
    long countByPostId(Long postId);
}
