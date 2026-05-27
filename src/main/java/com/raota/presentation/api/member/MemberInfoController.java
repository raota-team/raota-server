package com.raota.presentation.api.member;

import com.raota.presentation.api.member.contract.MemberInfoApi;
import com.raota.presentation.api.member.request.UpdateProfileRequest;
import com.raota.presentation.api.member.response.BookmarkSummaryResponse;
import com.raota.presentation.api.member.response.MyProfileResponse;
import com.raota.presentation.api.member.response.PhotoSummaryResponse;
import com.raota.presentation.api.member.response.VisitSummaryResponse;
import com.raota.application.member.MemberInfoService;
import com.raota.infrastructure.auth.LoginMember;
import com.raota.presentation.common.ApiResponse;
import com.raota.presentation.common.PageResponse;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/users")
public class MemberInfoController implements MemberInfoApi {
    private final MemberInfoService memberInfoService;

    @Override
    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<MyProfileResponse>> getUserProfile(
            @LoginMember Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(memberInfoService.getMyProfile(memberId)));
    }

    @Override
    @PatchMapping("/me/profile")
    public ResponseEntity<ApiResponse<MyProfileResponse>> updateMyProfile(
            @RequestBody UpdateProfileRequest request,
            @LoginMember Long memberId
    ) {
        MyProfileResponse updated = memberInfoService.updateMyProfile(
                request.getNickname(),
                request.getProfile_image_url(),
                request.getBackground_image_url(),
                request.getBio(),
                memberId
        );
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @Override
    @GetMapping("/me/photos")
    public ResponseEntity<ApiResponse<PageResponse<PhotoSummaryResponse>>> getUserPhoto(
            @LoginMember Long memberId,
            Pageable pageable) {
        Page<PhotoSummaryResponse> photos = memberInfoService.getMyPhotoList(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(photos)));
    }

    @Override
    @GetMapping("/me/bookmarks")
    public ResponseEntity<ApiResponse<PageResponse<BookmarkSummaryResponse>>> getMyBookmarks(
            @LoginMember Long memberId,
            Pageable pageable) {
        Page<BookmarkSummaryResponse> page = memberInfoService.getMyBookmarks(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    @Override
    @GetMapping("/me/visits")
    public ResponseEntity<ApiResponse<PageResponse<VisitSummaryResponse>>> getMyVisits(
            @LoginMember Long memberId,
            Pageable pageable) {
        Page<VisitSummaryResponse> page = memberInfoService.getMyVisits(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    @Override
    @GetMapping("/me/posts")
    public ResponseEntity<ApiResponse<PageResponse<com.raota.presentation.api.community.response.CommunityPostCardResponse>>> getMyPosts(
            @LoginMember Long memberId,
            @PageableDefault(size = 5, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<com.raota.presentation.api.community.response.CommunityPostCardResponse> response = memberInfoService.getMyPosts(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }

    @Override
    @GetMapping("/me/comments")
    public ResponseEntity<ApiResponse<PageResponse<com.raota.presentation.api.community.response.CommunityCommentItemResponse>>> getMyComments(
            @LoginMember Long memberId,
            @PageableDefault(size = 5, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<com.raota.presentation.api.community.response.CommunityCommentItemResponse> response = memberInfoService.getMyComments(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }

    @Override
    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<MyProfileResponse>> getUserProfileById(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(memberInfoService.getMyProfile(userId)));
    }

    @Override
    @GetMapping("/{userId}/photos")
    public ResponseEntity<ApiResponse<PageResponse<PhotoSummaryResponse>>> getUserPhotosById(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<PhotoSummaryResponse> photos = memberInfoService.getMyPhotoList(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(photos)));
    }

    @Override
    @GetMapping("/{userId}/visits")
    public ResponseEntity<ApiResponse<PageResponse<VisitSummaryResponse>>> getUserVisitsById(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<VisitSummaryResponse> page = memberInfoService.getMyVisits(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    @Override
    @GetMapping("/{userId}/posts")
    public ResponseEntity<ApiResponse<PageResponse<com.raota.presentation.api.community.response.CommunityPostCardResponse>>> getUserPostsById(
            @PathVariable Long userId,
            @PageableDefault(size = 5, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<com.raota.presentation.api.community.response.CommunityPostCardResponse> response = memberInfoService.getMyPosts(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }

    @Override
    @GetMapping("/{userId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<com.raota.presentation.api.community.response.CommunityCommentItemResponse>>> getUserCommentsById(
            @PathVariable Long userId,
            @PageableDefault(size = 5, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<com.raota.presentation.api.community.response.CommunityCommentItemResponse> response = memberInfoService.getMyComments(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }
}
