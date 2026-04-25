package com.raota.domain.community.service;

import com.raota.domain.community.model.Post;
import com.raota.domain.community.model.PostCategory;
import com.raota.domain.community.presentation.request.CommunityPostCreateRequest;
import com.raota.domain.community.repository.command.PostRepository;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.global.file.FileUploader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final FileUploader fileUploader;

    private static final Pattern BASE64_IMAGE_PATTERN = Pattern.compile("data:image/(?<ext>png|jpeg|jpg|gif|webp);base64,(?<data>[A-Za-z0-9+/=]+)");

    public Long createPost(
            CommunityPostCreateRequest request, 
            Long authorId
    ) {
        // 1. 본문 내 Base64 이미지를 CDN URL로 치환
        String processedContent = processContentImages(request.getContent());

        // 2. 도메인 모델 생성 및 저장
        Post post = Post.create(
                PostCategory.valueOf(request.getCategory()),
                request.getTitle(),
                processedContent,
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

    /**
     * 본문에서 Base64 이미지를 찾아 CDN에 업로드하고 URL로 치환한다.
     */
    private String processContentImages(String content) {
        if (content == null || content.isBlank()) return content;

        StringBuilder sb = new StringBuilder();
        Matcher matcher = BASE64_IMAGE_PATTERN.matcher(content);
        int lastIndex = 0;

        while (matcher.find()) {
            sb.append(content, lastIndex, matcher.start());
            
            String extension = matcher.group("ext");
            String base64Data = matcher.group("data");

            try {
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                String fileName = "post_" + UUID.randomUUID() + "." + extension;
                
                // MultipartFile로 변환하여 업로드
                CustomMultipartFile multipartFile = new CustomMultipartFile(imageBytes, fileName, "image/" + extension);
                String uploadedUrl = fileUploader.upload(multipartFile, "community");
                
                sb.append(uploadedUrl);
                log.info("Base64 image uploaded to CDN: {}", uploadedUrl);
            } catch (Exception e) {
                log.error("Failed to upload base64 image in content", e);
                sb.append(matcher.group(0)); // 실패 시 원래 Base64 유지
            }
            lastIndex = matcher.end();
        }
        sb.append(content.substring(lastIndex));
        return sb.toString();
    }

    /**
     * Base64 디코딩 데이터를 MultipartFile로 취급하기 위한 내부 헬퍼 클래스
     */
    private static class CustomMultipartFile implements MultipartFile {
        private final byte[] bytes;
        private final String name;
        private final String contentType;

        public CustomMultipartFile(byte[] bytes, String name, String contentType) {
            this.bytes = bytes;
            this.name = name;
            this.contentType = contentType;
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return bytes == null || bytes.length == 0; }
        @Override public long getSize() { return bytes.length; }
        @Override public byte[] getBytes() throws IOException { return bytes; }
        @Override public InputStream getInputStream() throws IOException { return new ByteArrayInputStream(bytes); }
        @Override public void transferTo(java.io.File dest) throws IOException { /* Not implemented */ }
    }


    public void deletePost(Long postId, Long authorId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        
        if (!post.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        postRepository.delete(postId);

        MemberProfile author = memberRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        author.decreasePostCount();
    }
}
