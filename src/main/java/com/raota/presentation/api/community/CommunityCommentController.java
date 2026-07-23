package com.raota.presentation.api.community;

import com.raota.application.community.service.CommentQueryService;
import com.raota.application.community.service.CommentService;
import com.raota.application.community.result.CommentItemResult;
import com.raota.application.community.result.CommentThreadResult;
import com.raota.infrastructure.auth.LoginMember;
import com.raota.presentation.api.community.contract.CommunityCommentApi;
import com.raota.presentation.api.community.request.CommunityCommentCreateRequest;
import com.raota.presentation.api.community.request.CommunityCommentUpdateRequest;
import com.raota.presentation.common.ApiResponse;
import com.raota.presentation.common.PageResponse;
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
