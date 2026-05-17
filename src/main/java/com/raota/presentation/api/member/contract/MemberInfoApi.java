package com.raota.presentation.api.member.contract;

import com.raota.presentation.api.member.dto.UpdateProfileRequest;
import com.raota.presentation.api.member.dto.BookmarkSummaryResponse;
import com.raota.presentation.api.member.dto.MyCommentSummaryResponse;
import com.raota.presentation.api.member.dto.MyPostSummaryResponse;
import com.raota.presentation.api.member.dto.MyProfileResponse;
import com.raota.presentation.api.member.dto.PhotoSummaryResponse;
import com.raota.presentation.api.member.dto.VisitSummaryResponse;
import com.raota.presentation.common.ApiResponse;
import com.raota.presentation.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "사용자", description = "사용자 프로필/활동 API")
public interface MemberInfoApi {

    @Operation(summary = "내 프로필 조회", description = "로그인 사용자의 프로필과 활동 통계를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<MyProfileResponse>> getUserProfile(
            @Parameter(hidden = true) Long memberId);

    @Operation(summary = "내 프로필 수정", description = "닉네임/프로필 이미지/백그라운드 이미지를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<MyProfileResponse>> updateMyProfile(
            UpdateProfileRequest request,
            @Parameter(hidden = true) Long memberId);

    @Operation(summary = "내 인증샷 목록 조회", description = "로그인 사용자의 인증샷 목록을 페이징으로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<PageResponse<PhotoSummaryResponse>>> getUserPhoto(
            @Parameter(hidden = true) Long memberId,
            @ParameterObject Pageable pageable);

    @Operation(summary = "내 북마크 목록 조회", description = "로그인 사용자의 북마크 목록을 페이징으로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<PageResponse<BookmarkSummaryResponse>>> getMyBookmarks(
            @Parameter(hidden = true) Long memberId,
            @ParameterObject Pageable pageable);

    @Operation(summary = "내 방문 목록 조회", description = "로그인 사용자의 방문 기록을 페이징으로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<PageResponse<VisitSummaryResponse>>> getMyVisits(
            @Parameter(hidden = true) Long memberId,
            @ParameterObject Pageable pageable);

    @Operation(summary = "내 글 목록 조회", description = "로그인 사용자의 글 목록을 페이징으로 조회합니다. 기본 페이지 크기는 5입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<PageResponse<com.raota.presentation.api.community.dto.CommunityPostCardResponse>>> getMyPosts(
            @Parameter(hidden = true) Long memberId,
            @ParameterObject Pageable pageable);

    @Operation(summary = "내 댓글 목록 조회", description = "댓글 내용과 댓글이 속한 글의 제목/작성일시를 페이징으로 조회합니다. 기본 페이지 크기는 5입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<PageResponse<com.raota.presentation.api.community.dto.CommunityCommentItemResponse>>> getMyComments(
            @Parameter(hidden = true) Long memberId,
            @ParameterObject Pageable pageable);

    @Operation(summary = "사용자 프로필 조회", description = "특정 사용자의 프로필과 활동 통계를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<MyProfileResponse>> getUserProfileById(
            @PathVariable Long userId);

    @Operation(summary = "사용자 인증샷 목록 조회", description = "특정 사용자의 인증샷 목록을 페이징으로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<PageResponse<PhotoSummaryResponse>>> getUserPhotosById(
            @PathVariable Long userId,
            @ParameterObject Pageable pageable);

    @Operation(summary = "사용자 방문 목록 조회", description = "특정 사용자의 방문 기록을 페이징으로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<PageResponse<VisitSummaryResponse>>> getUserVisitsById(
            @PathVariable Long userId,
            @ParameterObject Pageable pageable);

    @Operation(summary = "사용자 글 목록 조회", description = "특정 사용자의 글 목록을 페이징으로 조회합니다. 기본 페이지 크기는 5입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<PageResponse<com.raota.presentation.api.community.dto.CommunityPostCardResponse>>> getUserPostsById(
            @PathVariable Long userId,
            @ParameterObject Pageable pageable);

    @Operation(summary = "사용자 댓글 목록 조회", description = "특정 사용자의 댓글 목록을 페이징으로 조회합니다. 기본 페이지 크기는 5입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<PageResponse<com.raota.presentation.api.community.dto.CommunityCommentItemResponse>>> getUserCommentsById(
            @PathVariable Long userId,
            @ParameterObject Pageable pageable);
}
