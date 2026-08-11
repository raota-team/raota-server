package com.raota.community.presentation;

import com.raota.account.application.member.MemberActivityVisibilityService;
import com.raota.account.infrastructure.auth.LoginMember;
import com.raota.community.application.result.CommentItemResult;
import com.raota.community.application.result.PostCardResult;
import com.raota.community.application.service.CommentQueryService;
import com.raota.community.application.service.PostQueryService;
import com.raota.global.presentation.common.ApiResponse;
import com.raota.global.presentation.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자", description = "사용자 프로필/활동 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class MemberCommunityActivityController {

    private final PostQueryService postQueryService;
    private final CommentQueryService commentQueryService;
    private final MemberActivityVisibilityService memberActivityVisibilityService;

    @Operation(summary = "내 글 목록 조회", description = "로그인 사용자의 글 목록을 페이징으로 조회합니다. 기본 페이지 크기는 5입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    @GetMapping("/me/posts")
    public ResponseEntity<ApiResponse<PageResponse<PostCardResult>>> getMyPosts(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @ParameterObject @PageableDefault(size = 5, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PostCardResult> response = postQueryService.findPostCardsByAuthor(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }

    @Operation(summary = "내 댓글 목록 조회", description = "댓글 내용과 댓글이 속한 글의 제목/작성일시를 페이징으로 조회합니다. 기본 페이지 크기는 5입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    @GetMapping("/me/comments")
    public ResponseEntity<ApiResponse<PageResponse<CommentItemResult>>> getMyComments(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @ParameterObject @PageableDefault(size = 5, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CommentItemResult> response = commentQueryService.findCommentsByAuthor(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }

    @Operation(summary = "사용자 글 목록 조회", description = "특정 사용자의 글 목록을 페이징으로 조회합니다. 기본 페이지 크기는 5입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    @GetMapping("/{userId}/posts")
    public ResponseEntity<ApiResponse<PageResponse<PostCardResult>>> getUserPostsById(
            @PathVariable Long userId,
            @Parameter(hidden = true) @LoginMember(required = false) Long viewerId,
            @ParameterObject @PageableDefault(size = 5, direction = Sort.Direction.DESC) Pageable pageable) {
        memberActivityVisibilityService.requirePostsVisible(userId, viewerId);
        Page<PostCardResult> response = postQueryService.findPostCardsByAuthor(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }

    @Operation(summary = "사용자 댓글 목록 조회", description = "특정 사용자의 댓글 목록을 페이징으로 조회합니다. 기본 페이지 크기는 5입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    @GetMapping("/{userId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<CommentItemResult>>> getUserCommentsById(
            @PathVariable Long userId,
            @Parameter(hidden = true) @LoginMember(required = false) Long viewerId,
            @ParameterObject @PageableDefault(size = 5, direction = Sort.Direction.DESC) Pageable pageable) {
        memberActivityVisibilityService.requireCommentsVisible(userId, viewerId);
        Page<CommentItemResult> response = commentQueryService.findCommentsByAuthor(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }
}
