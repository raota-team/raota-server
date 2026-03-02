package com.raota.domain.community.presentation.contract;

import com.raota.domain.community.presentation.request.CommunityCommentCreateRequest;
import com.raota.domain.community.presentation.request.CommunityCommentUpdateRequest;
import com.raota.domain.community.presentation.response.CommunityCommentItemResponse;
import com.raota.domain.community.presentation.response.CommunityCommentThreadResponse;
import com.raota.global.common.ApiResponse;
import com.raota.global.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "CommunityComment", description = "커뮤니티 댓글 API")
public interface CommunityCommentApi {

    @Operation(summary = "커뮤니티 댓글 목록 조회",
            description = "글의 댓글 목록을 조회합니다. 답글은 depth 1까지만 포함합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<PageResponse<CommunityCommentThreadResponse>>> getComments(
            @Parameter(description = "글 ID", required = true) Long postId,
            @ParameterObject Pageable pageable);

    @Operation(summary = "커뮤니티 댓글 작성",
            description = "댓글 또는 답글을 작성합니다. 답글 작성 시 부모 댓글 작성자가 태그됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<CommunityCommentItemResponse>> createComment(
            @Parameter(description = "글 ID", required = true) Long postId,
            CommunityCommentCreateRequest request,
            @Parameter(hidden = true) Long memberId);

    @Operation(summary = "커뮤니티 댓글 수정",
            description = "댓글 내용을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<CommunityCommentItemResponse>> updateComment(
            @Parameter(description = "댓글 ID", required = true) Long commentId,
            CommunityCommentUpdateRequest request,
            @Parameter(hidden = true) Long memberId);

    @Operation(summary = "커뮤니티 댓글 삭제",
            description = "댓글을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(description = "댓글 ID", required = true) Long commentId,
            @Parameter(hidden = true) Long memberId);
}
