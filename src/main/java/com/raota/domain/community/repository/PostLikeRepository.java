package com.raota.domain.community.repository;

public interface PostLikeRepository {
    boolean toggle(Long postId, Long memberId);
    long countByPostId(Long postId);
}
