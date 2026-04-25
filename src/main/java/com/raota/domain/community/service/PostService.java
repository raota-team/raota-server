package com.raota.domain.community.service;

import com.raota.domain.community.model.Post;
import com.raota.domain.community.model.PostCategory;
import com.raota.domain.community.presentation.request.CommunityPostCreateRequest;
import com.raota.domain.community.repository.command.PostRepository;
import com.raota.domain.community.repository.command.entity.PostEntity;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.global.file.FileUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final RamenShopRepository ramenShopRepository;
    private final FileUploader fileUploader;

    public Long createPost(
            CommunityPostCreateRequest request, 
            Long authorId
    ) {
        // 도메인 모델 생성 및 저장 (프론트에서 URL로 보내줌)
        Post post = Post.create(
                PostCategory.valueOf(request.getCategory()),
                request.getTitle(),
                request.getContent(),
                request.getContentFormat(),
                request.getThumbnailUrl(),
                authorId,
                request.getRamenShopId()
        );

        Post savedPost = postRepository.save(post);
        
        if (postRepository instanceof com.raota.domain.community.repository.command.JpaPostRepository jpaRepo) {
            jpaRepo.flush();
        }

        Long postId = savedPost.getId();

        MemberProfile author = memberRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        author.increasePostCount();

        return postId;
    }

    public void updatePost(Long postId, CommunityPostCreateRequest request, Long authorId) {
        PostEntity postEntity = postRepository.findEntityById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!postEntity.getAuthor().getId().equals(authorId)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        com.raota.domain.ramenShop.model.RamenShop ramenShop = null;
        if (request.getRamenShopId() != null) {
            ramenShop = ramenShopRepository.findById(request.getRamenShopId())
                    .orElseThrow(() -> new IllegalArgumentException("없는 라멘집 입니다."));
        }

        postEntity.update(
                PostCategory.valueOf(request.getCategory()),
                request.getTitle(),
                request.getContent(),
                request.getThumbnailUrl(),
                ramenShop
        );
    }

    public void deletePost(Long postId, Long authorId) {
        PostEntity postEntity = postRepository.findEntityById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        
        if (!postEntity.getAuthor().getId().equals(authorId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        postEntity.delete();

        MemberProfile author = memberRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        author.decreasePostCount();
    }
}
