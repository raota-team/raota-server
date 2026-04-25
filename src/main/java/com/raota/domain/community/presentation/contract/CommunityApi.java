package com.raota.domain.community.presentation.contract;

import com.raota.domain.community.presentation.request.CommunityPostCreateRequest;
import com.raota.domain.community.presentation.request.CommunityPostSearchRequest;
import com.raota.domain.community.presentation.request.CommunityRamenShopSearchRequest;
import com.raota.domain.community.presentation.response.CommunityPostCardResponse;
import com.raota.domain.community.presentation.response.CommunityPostDetailResponse;
import com.raota.domain.community.presentation.response.CommunityRamenShopOptionResponse;
import com.raota.global.common.ApiResponse;
import com.raota.global.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "커뮤니티", description = "커뮤니티 API")
public interface CommunityApi {

    @Operation(summary = "커뮤니티 글 목록 조회",
            description = "카테고리 필터와 페이징으로 커뮤니티 글 목록을 조회합니다. 기본 페이지 크기는 10입니다.")
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

    @Operation(summary = "커뮤니티 글 작성용 라멘집 목록 조회",
            description = "맛집후기 카테고리 작성 시 선택할 라멘집 목록을 검색/페이징으로 조회합니다. 기본 페이지 크기는 10입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<ApiResponse<PageResponse<CommunityRamenShopOptionResponse>>> getRamenShopOptions(
            @ParameterObject Pageable pageable,
            @ParameterObject CommunityRamenShopSearchRequest request);
}
