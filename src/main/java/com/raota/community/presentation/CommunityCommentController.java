package com.raota.community.presentation;

import com.raota.community.application.service.CommentQueryService;
import com.raota.community.application.service.CommentService;
import com.raota.community.application.result.CommentItemResult;
import com.raota.community.application.result.CommentThreadResult;
import com.raota.account.infrastructure.auth.LoginMember;
import com.raota.community.presentation.contract.CommunityCommentApi;
import com.raota.community.presentation.request.CommunityCommentCreateRequest;
import com.raota.community.presentation.request.CommunityCommentUpdateRequest;
import com.raota.global.presentation.common.ApiResponse;
import com.raota.global.presentation.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
public class CommunityCommentController implements CommunityCommentApi {

    private final CommentService commentService;
    private final CommentQueryService commentQueryService;

    @Override
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentItemResult>> createComment(
            @PathVariable Long postId,
            @RequestBody CommunityCommentCreateRequest request,
            @LoginMember Long memberId) {
        Long commentId = commentService.createComment(request.toCommand(postId, memberId));
        return ResponseEntity.ok(ApiResponse.success(commentQueryService.getComment(commentId)));
    }

    @Override
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<CommentThreadResult>>> getComments(
            @PathVariable Long postId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                commentQueryService.getCommentThreads(postId, pageable)
        )));
    }

    @Override
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentItemResult>> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommunityCommentUpdateRequest request,
            @LoginMember Long memberId) {
        commentService.updateComment(request.toCommand(commentId, memberId));
        return ResponseEntity.ok(ApiResponse.success(commentQueryService.getComment(commentId)));
    }

    @Override
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @LoginMember Long memberId) {
        commentService.deleteComment(commentId, memberId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
