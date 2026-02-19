package com.raota.domain.community.presentation.controller;

import com.raota.domain.community.presentation.contract.CommunityCommentApi;
import com.raota.domain.community.presentation.request.CommunityCommentCreateRequest;
import com.raota.domain.community.presentation.request.CommunityCommentUpdateRequest;
import com.raota.domain.community.presentation.response.CommunityCommentItemResponse;
import com.raota.domain.community.presentation.response.CommunityCommentThreadResponse;
import com.raota.global.auth.LoginMember;
import com.raota.global.common.ApiResponse;
import org.springframework.data.domain.Page;
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
public class CommunityCommentController implements CommunityCommentApi {

    @Override
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<Page<CommunityCommentThreadResponse>>> getComments(
            @PathVariable Long postId,
            @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(Page.empty(pageable)));
    }

    @Override
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommunityCommentItemResponse>> createComment(
            @PathVariable Long postId,
            @RequestBody CommunityCommentCreateRequest request,
            @LoginMember Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Override
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommunityCommentItemResponse>> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommunityCommentUpdateRequest request,
            @LoginMember Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Override
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @LoginMember Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
