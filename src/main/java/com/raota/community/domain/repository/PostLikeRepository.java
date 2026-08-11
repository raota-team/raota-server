package com.raota.community.domain.repository;

public interface PostLikeRepository {
    boolean toggle(Long postId, Long memberId);
    long countByPostId(Long postId);
}
