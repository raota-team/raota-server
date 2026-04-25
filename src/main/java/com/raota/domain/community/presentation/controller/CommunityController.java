package com.raota.domain.community.presentation.controller;

import com.raota.domain.community.presentation.contract.CommunityApi;
import com.raota.domain.community.presentation.request.CommunityPostCreateRequest;
import com.raota.domain.community.presentation.request.CommunityPostSearchRequest;
import com.raota.domain.community.presentation.request.CommunityRamenShopSearchRequest;
import com.raota.domain.community.presentation.response.CommunityPostCardResponse;
import com.raota.domain.community.presentation.response.CommunityPostDetailResponse;
import com.raota.domain.community.presentation.response.CommunityRamenShopOptionResponse;
import com.raota.domain.community.repository.query.PostQueryRepository;
import com.raota.domain.community.service.PostService;
import com.raota.global.auth.LoginMember;
import com.raota.global.common.ApiResponse;
import com.raota.global.common.PageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
public class CommunityController implements CommunityApi {

    private final PostService postService;
    private final PostQueryRepository postQueryRepository;

    @Override
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<PageResponse<CommunityPostCardResponse>>> getCommunityPosts(
            @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable,
            CommunityPostSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(postQueryRepository.searchPostCards(request, pageable)));
    }

    @Override
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<CommunityPostDetailResponse>> getCommunityPostDetail(
            @PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success(postQueryRepository.getPostDetail(postId)));
    }

    @Override
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<CommunityPostDetailResponse>> createCommunityPost(
            @org.springframework.web.bind.annotation.RequestBody CommunityPostCreateRequest request,
            @LoginMember Long memberId) {
        
        Long postId = postService.createPost(request, memberId);
        return ResponseEntity.ok(ApiResponse.success(postQueryRepository.getPostDetail(postId)));
    }

    @Override
    @GetMapping("/ramen-shops")
    public ResponseEntity<ApiResponse<PageResponse<CommunityRamenShopOptionResponse>>> getRamenShopOptions(
            @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable,
            CommunityRamenShopSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(postQueryRepository.getRamenShopOptions(request, pageable)));
    }
}
