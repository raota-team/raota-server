package com.raota.domain.community.service;

import com.raota.domain.community.model.Post;
import com.raota.domain.community.model.PostCategory;
import com.raota.domain.community.presentation.request.CommunityPostCreateRequest;
import com.raota.domain.community.repository.command.PostRepository;
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
    private final FileUploader fileUploader;

    public Long createPost(
            CommunityPostCreateRequest request, 
            MultipartFile thumbnailFile, 
            List<MultipartFile> contentImages, 
            Long authorId
    ) {
        // 1. 이미지 업로드 처리 (S3 등)
        String thumbnailUrl = request.getThumbnailUrl();
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            thumbnailUrl = fileUploader.upload(thumbnailFile);
        }

        // 본문 이미지들의 URL 추출 (필요 시 본문 내용 치환 로직 추가 가능)
        if (contentImages != null && !contentImages.isEmpty()) {
            List<String> imageUrls = contentImages.stream()
                    .map(file -> file.isEmpty() ? null : fileUploader.upload(file))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            // TODO: 명세서에 따라 본문 내 이미지 URL을 imageUrls로 치환하는 로직을 넣을 수 있습니다.
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

        return postRepository.save(post).getId();
    }

    public void deletePost(Long postId, Long authorId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        
        if (!post.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        postRepository.delete(postId);
    }
}
