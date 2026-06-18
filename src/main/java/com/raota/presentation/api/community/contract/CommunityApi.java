package com.raota.presentation.api.community.contract;

import com.raota.presentation.api.community.request.CommunityPostCreateRequest;
import com.raota.presentation.api.community.request.CommunityPostSearchRequest;
import com.raota.presentation.api.community.request.CommunityRamenShopSearchRequest;
import com.raota.presentation.api.community.response.CommunityPostCardResponse;
import com.raota.presentation.api.community.response.CommunityPostDetailResponse;
import com.raota.presentation.api.community.response.CommunityRamenShopOptionResponse;
import com.raota.presentation.common.ApiResponse;
import com.raota.presentation.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "커뮤니티", description = "커뮤니티 API")
public interface CommunityApi {

    @Operation(summary = "커뮤니티 글 목록 조회",
            description = "카테고리, 라멘집 ID 필터와 페이징으로 커뮤니티 글 목록을 조회합니다. category=POPULAR이면 좋아요 3개 이상인 글을 최신순으로 조회합니다. 기본 페이지 크기는 10입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<PageResponse<CommunityPostCardResponse>>> getCommunityPosts(
            @ParameterObject Pageable pageable,
            @ParameterObject CommunityPostSearchRequest request);

    @Operation(summary = "커뮤니티 글 상세 조회",
            description = "커뮤니티 글 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<CommunityPostDetailResponse>> getCommunityPostDetail(
            @Parameter(description = "글 ID", required = true) Long postId,
            @Parameter(hidden = true) Long memberId);

    @Operation(summary = "커뮤니티 글 조회수 증가",
            description = "커뮤니티 게시글의 조회수를 1 증가시킵니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<Void>> increasePostViewCount(
            @Parameter(description = "글 ID", required = true) Long postId);

    @Operation(summary = "커뮤니티 글 작성",
            description = "JSON 형식으로 커뮤니티 글을 작성합니다. 이미지는 사전 업로드 후 URL로 포함되어야 합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<CommunityPostDetailResponse>> createCommunityPost(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "글 작성 요청") 
            CommunityPostCreateRequest request,
            @Parameter(hidden = true) Long memberId);

    @Operation(summary = "커뮤니티 글 수정",
            description = "본인이 작성한 글을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<CommunityPostDetailResponse>> updateCommunityPost(
            @Parameter(description = "글 ID") Long postId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "글 수정 요청") 
            CommunityPostCreateRequest request,
            @Parameter(hidden = true) Long memberId);

    @Operation(summary = "커뮤니티 글 삭제",
            description = "본인이 작성한 글을 삭제(소프트 딜리트)합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<Void>> deleteCommunityPost(
            @Parameter(description = "글 ID") Long postId,
            @Parameter(hidden = true) Long memberId);

    @Operation(summary = "커뮤니티 글 좋아요 토글",
            description = "게시글에 좋아요를 누르거나 취소합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<Boolean>> togglePostLike(
            @Parameter(description = "글 ID") Long postId,
            @Parameter(hidden = true) Long memberId);

    @Operation(summary = "커뮤니티 글 작성용 라멘집 목록 조회",
            description = "맛집후기 카테고리 작성 시 선택할 라멘집 목록을 검색/페이징으로 조회합니다. 기본 페이지 크기는 10입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<PageResponse<CommunityRamenShopOptionResponse>>> getRamenShopOptions(
            @ParameterObject Pageable pageable,
            @ParameterObject CommunityRamenShopSearchRequest request);
}
