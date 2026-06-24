package com.raota.application.community.service;

import com.raota.domain.community.model.Post;
import com.raota.domain.community.model.PostCategory;
import com.raota.presentation.api.community.request.CommunityPostCreateRequest;
import com.raota.domain.community.repository.command.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommunityService {
    private final PostRepository postRepository;

    public Long createPost(CommunityPostCreateRequest request, Long authorId, String thumbnailUrl) {
        Post post = Post.create(
                PostCategory.valueOf(request.getCategory()),
                request.getTitle(),
                request.getContent(),
                request.getContentFormat(),
                thumbnailUrl != null ? thumbnailUrl : request.getThumbnailUrl(),
                authorId,
                request.getRamenShopId()
        );

        return postRepository.save(post).getId();
    }

    @Transactional(readOnly = true)
    public Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. ID: " + postId));
    }
}
