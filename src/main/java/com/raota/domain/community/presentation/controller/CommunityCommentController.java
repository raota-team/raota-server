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
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
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
    private final CommentQueryRepository commentQueryRepository;

    @Override
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommunityCommentItemResponse>> createComment(
            @PathVariable Long postId,
            @RequestBody CommunityCommentCreateRequest request,
            @LoginMember Long memberId) {
        Long commentId = commentService.createComment(postId, request, memberId);
        CommunityCommentItemResponse response = commentQueryRepository.getComment(commentId)
                .orElseThrow(() -> new IllegalStateException("생성된 댓글을 찾을 수 없습니다."));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<CommunityCommentThreadResponse>>> getComments(
            @PathVariable Long postId,
            Pageable pageable) {
        PageResponse<CommunityCommentItemResponse> itemResponse = commentQueryRepository.getComments(postId, pageable);
        
        // ItemResponse -> ThreadResponse 변환 (답글은 일단 빈 리스트로 처리)
        List<CommunityCommentThreadResponse> threads = itemResponse.items().stream()
                .map(item -> new CommunityCommentThreadResponse(
                        item.commentId(),
                        item.authorNickname(),
                        item.createdAt(),
                        item.content(),
                        Collections.emptyList()
                ))
                .toList();
        
        PageResponse<CommunityCommentThreadResponse> response = PageResponse.from(
                new PageImpl<>(threads, pageable, itemResponse.page().totalElements())
        );
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommunityCommentItemResponse>> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommunityCommentUpdateRequest request,
            @LoginMember Long memberId) {
        commentService.updateComment(commentId, request, memberId);
        CommunityCommentItemResponse response = commentQueryRepository.getComment(commentId)
                .orElseThrow(() -> new IllegalStateException("수정된 댓글을 찾을 수 없습니다."));
        return ResponseEntity.ok(ApiResponse.success(response));
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
