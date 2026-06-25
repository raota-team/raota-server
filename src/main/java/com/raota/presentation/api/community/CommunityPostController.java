package com.raota.presentation.api.community;

import com.raota.application.community.service.PostLikeService;
import com.raota.application.community.service.PostQueryService;
import com.raota.application.community.service.PostService;
import com.raota.infrastructure.auth.LoginMember;
import com.raota.presentation.api.community.contract.CommunityApi;
import com.raota.presentation.api.community.request.CommunityCreatePostRequest;
import com.raota.presentation.api.community.request.CommunityPostSearchRequest;
import com.raota.presentation.api.community.request.CommunityRamenShopSearchRequest;
import com.raota.presentation.api.community.request.CommunityUpdatePostRequest;
import com.raota.presentation.api.community.response.CommunityPostCardResponse;
import com.raota.presentation.api.community.response.CommunityPostDetailResponse;
import com.raota.presentation.api.community.response.CommunityRamenShopOptionResponse;
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
public class CommunityPostController implements CommunityApi {

    private final PostService postService;
    private final PostLikeService postLikeService;
    private final PostQueryService postQueryService;

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
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                postQueryService.searchPostCards(request.toQuery(), pageable)
                        .map(CommunityPostCardResponse::from)
        )));
    }

    @Override
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<CommunityPostDetailResponse>> getCommunityPostDetail(
            @PathVariable Long postId,
            @LoginMember(required = false) Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(CommunityPostDetailResponse.from(
                postQueryService.getPostDetail(postId, memberId)
        )));
    }

    @Override
    @PostMapping("/posts/{postId}/views")
    public ResponseEntity<ApiResponse<Void>> increasePostViewCount(
            @PathVariable Long postId) {
        postService.increaseViewCount(postId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Override
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<CommunityPostDetailResponse>> createCommunityPost(
            @RequestBody CommunityCreatePostRequest request,
            @LoginMember Long memberId) {

        Long postId = postService.createPost(request.toCommand(memberId));
        return ResponseEntity.ok(ApiResponse.success(CommunityPostDetailResponse.from(
                postQueryService.getPostDetail(postId, memberId)
        )));
    }

    @PatchMapping("/posts/{postId}")
    @Override
    public ResponseEntity<ApiResponse<CommunityPostDetailResponse>> updateCommunityPost(
            @PathVariable Long postId,
            @RequestBody CommunityUpdatePostRequest request,
            @LoginMember Long memberId) {
        postService.updatePost(request.toCommand(postId, memberId));
        return ResponseEntity.ok(ApiResponse.success(CommunityPostDetailResponse.from(
                postQueryService.getPostDetail(postId, memberId)
        )));
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
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                postQueryService.getRamenShopOptions(request.toQuery(), pageable)
                        .map(CommunityRamenShopOptionResponse::from)
        )));
    }
}
