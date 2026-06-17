package com.raota.presentation.api.community;

import com.raota.presentation.api.community.contract.CommunityApi;
import com.raota.presentation.api.community.request.CommunityPostCreateRequest;
import com.raota.presentation.api.community.request.CommunityPostSearchRequest;
import com.raota.presentation.api.community.request.CommunityRamenShopSearchRequest;
import com.raota.presentation.api.community.response.CommunityPostCardResponse;
import com.raota.presentation.api.community.response.CommunityPostDetailResponse;
import com.raota.presentation.api.community.response.CommunityRamenShopOptionResponse;
import com.raota.domain.community.repository.query.PostQueryRepository;
import com.raota.application.community.PostLikeService;
import com.raota.application.community.PostService;
import com.raota.infrastructure.auth.LoginMember;
import com.raota.presentation.common.ApiResponse;
import com.raota.presentation.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
public class CommunityController implements CommunityApi {

    private final PostService postService;
    private final PostLikeService postLikeService;
    private final PostQueryRepository postQueryRepository;

    @Override
    @PostMapping("/posts/{postId}/likes")
    public ResponseEntity<ApiResponse<Boolean>> togglePostLike(
            @PathVariable Long postId,
            @LoginMember Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(postLikeService.toggleLike(postId, memberId)));
    }

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
            @PathVariable Long postId,
            @LoginMember(required = false) Long memberId) {
        postService.increaseViewCount(postId);
        return ResponseEntity.ok(ApiResponse.success(postQueryRepository.getPostDetail(postId, memberId)));
    }

    @Override
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<CommunityPostDetailResponse>> createCommunityPost(
            @RequestBody CommunityPostCreateRequest request,
            @LoginMember Long memberId) {
        
        Long postId = postService.createPost(request, memberId);
        return ResponseEntity.ok(ApiResponse.success(postQueryRepository.getPostDetail(postId, memberId)));
    }

    @Override
    @PatchMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<CommunityPostDetailResponse>> updateCommunityPost(
            @PathVariable Long postId,
            @RequestBody CommunityPostCreateRequest request,
            @LoginMember Long memberId) {
        postService.updatePost(postId, request, memberId);
        return ResponseEntity.ok(ApiResponse.success(postQueryRepository.getPostDetail(postId, memberId)));
    }

    @Override
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deleteCommunityPost(
            @PathVariable Long postId,
            @LoginMember Long memberId) {
        postService.deletePost(postId, memberId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Override
    @GetMapping("/ramen-shops")
    public ResponseEntity<ApiResponse<PageResponse<CommunityRamenShopOptionResponse>>> getRamenShopOptions(
            @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable,
            CommunityRamenShopSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(postQueryRepository.getRamenShopOptions(request, pageable)));
    }
}
