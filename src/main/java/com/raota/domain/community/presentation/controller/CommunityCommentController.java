package com.raota.domain.community.presentation.controller;

import com.raota.domain.community.presentation.contract.CommunityCommentApi;
import com.raota.domain.community.presentation.request.CommunityCommentCreateRequest;
import com.raota.domain.community.presentation.request.CommunityCommentUpdateRequest;
import com.raota.domain.community.presentation.response.CommunityCommentItemResponse;
import com.raota.domain.community.presentation.response.CommunityCommentThreadResponse;
import com.raota.domain.community.repository.query.CommentQueryRepository;
import com.raota.domain.community.service.CommentService;
import com.raota.global.auth.LoginMember;
import com.raota.global.common.ApiResponse;
import com.raota.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
public class CommunityCommentController implements CommunityCommentApi {

    private final CommentService commentService;

    @Override
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<Void>> createComment(
            @PathVariable Long postId,
            @RequestBody CommunityCommentCreateRequest request,
            @LoginMember Long memberId) {
        commentService.createComment(postId, request, memberId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Override
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<CommunityCommentItemResponse>>> getComments(
            @PathVariable Long postId,
            Pageable pageable) {
        // TODO: CommentQueryRepository를 사용하여 조회 구현
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Override
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @LoginMember Long memberId) {
        commentService.deleteComment(commentId, memberId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> updateComment(Long commentId, CommunityCommentUpdateRequest request, Long memberId) {
        // TODO: 댓글 수정 로직 필요 시 추가
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Override
    public ResponseEntity<ApiResponse<CommunityCommentThreadResponse>> getCommentThread(Long commentId) {
        // TODO: 대댓글(Thread) 조회 로직 필요 시 추가
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
