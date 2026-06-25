package com.raota.application.community.service;

import com.raota.domain.community.repository.PostLikeRepository;
import com.raota.domain.community.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostLikeService {
    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;

    /**
     * 좋아요 토글 (있으면 삭제, 없으면 생성)
     * @return 현재 좋아요 상태 (true: 좋아요함, false: 취소함)
     */
    public boolean toggleLike(Long postId, Long memberId) {
        postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        return postLikeRepository.toggle(postId, memberId);
    }
}
