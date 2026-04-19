package com.raota.domain.community.service;

import com.raota.domain.community.model.Post;
import com.raota.domain.community.model.PostCategory;
import com.raota.domain.community.presentation.request.CommunityPostCreateRequest;
import com.raota.domain.community.repository.command.PostRepository;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.global.file.FileUploader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final FileUploader fileUploader;

    public Long createPost(
            CommunityPostCreateRequest request, 
            MultipartFile thumbnailFile, 
            List<MultipartFile> contentImages, 
            Long authorId
    ) {
        // 1. 이미지 업로드 처리
        String thumbnailUrl = request.getThumbnailUrl();
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            thumbnailUrl = fileUploader.upload(thumbnailFile, "community");
        }

        if (contentImages != null && !contentImages.isEmpty()) {
            contentImages.stream()
                    .map(file -> file.isEmpty() ? null : fileUploader.upload(file, "community"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // 2. 도메인 모델 생성 및 저장
        Post post = Post.create(
                PostCategory.valueOf(request.getCategory()),
                request.getTitle(),
                request.getContent(),
                request.getContentFormat(),
                thumbnailUrl,
                authorId,
                request.getRamenShopId()
        );

        Post savedPost = postRepository.save(post);
        // JPA 영속성 컨텍스트의 변경 내용을 DB에 즉시 반영하여 JOOQ 등 다른 기술에서 조회 가능하게 한다.
        if (postRepository instanceof com.raota.domain.community.repository.command.JpaPostRepository jpaRepo) {
            jpaRepo.flush();
        }

        Long postId = savedPost.getId();

        // 3. 마이페이지 통계 업데이트 (추가!)
        MemberProfile author = memberRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        author.increasePostCount();

        return postId;
    }

    public void deletePost(Long postId, Long authorId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        
        if (!post.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        postRepository.delete(postId);

        // 4. 마이페이지 통계 업데이트 (삭제 시 감소!)
        MemberProfile author = memberRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        author.decreasePostCount();
    }
}
