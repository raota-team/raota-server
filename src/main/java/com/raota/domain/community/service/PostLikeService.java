package com.raota.domain.community.service;

import com.raota.domain.community.repository.command.PostLikeRepository;
import com.raota.domain.community.repository.command.PostRepository;
import com.raota.domain.community.repository.command.entity.PostLikeEntity;
import java.util.Optional;
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

        Optional<PostLikeEntity> existingLike = postLikeRepository.findByPostIdAndMemberId(postId, memberId);

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            return false;
        } else {
            postLikeRepository.save(PostLikeEntity.builder()
                    .postId(postId)
                    .memberId(memberId)
                    .build());
            return true;
        }
    }
}
