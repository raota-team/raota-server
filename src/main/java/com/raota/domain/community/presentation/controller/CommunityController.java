package com.raota.domain.community.presentation.controller;

import com.raota.domain.community.presentation.contract.CommunityApi;
import com.raota.domain.community.presentation.request.CommunityPostCreateRequest;
import com.raota.domain.community.presentation.request.CommunityPostSearchRequest;
import com.raota.domain.community.presentation.request.CommunityRamenShopSearchRequest;
import com.raota.domain.community.presentation.response.CommunityPostCardResponse;
import com.raota.domain.community.presentation.response.CommunityPostDetailResponse;
import com.raota.domain.community.presentation.response.CommunityRamenShopOptionResponse;
import com.raota.global.auth.LoginMember;
import com.raota.global.common.ApiResponse;
import com.raota.global.common.PageResponse;
import java.util.List;
import org.springframework.data.domain.Page;
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
public class CommunityController implements CommunityApi {

    @Override
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<PageResponse<CommunityPostCardResponse>>> getCommunityPosts(
            @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable,
            CommunityPostSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(Page.empty(pageable))));
    }

    @Override
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<CommunityPostDetailResponse>> getCommunityPostDetail(
            @PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Override
    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CommunityPostDetailResponse>> createCommunityPost(
            @RequestPart("request") CommunityPostCreateRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(value = "contentImages", required = false) List<MultipartFile> contentImages,
            @LoginMember Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Override
    @GetMapping("/ramen-shops")
    public ResponseEntity<ApiResponse<PageResponse<CommunityRamenShopOptionResponse>>> getRamenShopOptions(
            @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable,
            CommunityRamenShopSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(Page.empty(pageable))));
    }
}
