package com.raota.discovery.presentation.member;

import com.raota.community.application.result.CommentItemResult;
import com.raota.community.application.result.PostCardResult;
import com.raota.discovery.presentation.member.contract.MemberInfoApi;
import com.raota.account.presentation.member.request.UpdateEmailRequest;
import com.raota.account.presentation.member.request.UpdateProfileRequest;
import com.raota.account.presentation.member.request.ActivityVisibilityUpdateRequest;
import com.raota.account.presentation.member.response.BookmarkSummaryResponse;
import com.raota.account.presentation.member.response.MemberSummaryResponse;
import com.raota.account.presentation.member.response.MyProfileResponse;
import com.raota.account.presentation.member.response.PhotoSummaryResponse;
import com.raota.account.presentation.member.response.VisitSummaryResponse;
import com.raota.account.presentation.member.response.ActivityVisibilityResponse;
import com.raota.account.application.member.MemberLifecycleService;
import com.raota.discovery.application.member.MemberInfoService;
import com.raota.account.application.member.MemberActivityVisibilityService;
import com.raota.account.infrastructure.auth.LoginMember;
import com.raota.account.infrastructure.auth.RefreshTokenCookieManager;
import com.raota.global.presentation.common.ApiResponse;
import com.raota.global.presentation.common.PageResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@AllArgsConstructor
@RestController
@RequestMapping("/users")
public class MemberInfoController implements MemberInfoApi {
    private final MemberInfoService memberInfoService;
    private final MemberLifecycleService memberLifecycleService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;
    private final MemberActivityVisibilityService memberActivityVisibilityService;

    @Override
    @GetMapping("/me/summary")
    public ResponseEntity<ApiResponse<MemberSummaryResponse>> getMySummary(
            @LoginMember Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(memberInfoService.getMemberSummary(memberId)));
    }

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
    @PatchMapping("/me/email")
    public ResponseEntity<ApiResponse<MyProfileResponse>> updateMyEmail(
            @Valid @RequestBody UpdateEmailRequest request,
            @LoginMember Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberInfoService.updateMyEmail(request.email(), memberId)));
    }

    @GetMapping("/me/privacy-settings")
    public ResponseEntity<ApiResponse<ActivityVisibilityResponse>> getPrivacySettings(
            @LoginMember Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberActivityVisibilityService.get(memberId)));
    }

    @PatchMapping("/me/privacy-settings")
    public ResponseEntity<ApiResponse<ActivityVisibilityResponse>> updatePrivacySettings(
            @Valid @RequestBody ActivityVisibilityUpdateRequest request,
            @LoginMember Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberActivityVisibilityService.update(memberId, request)));
    }

    @Override
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdrawMyAccount(
            @LoginMember Long memberId,
            HttpServletResponse response
    ) {
        memberLifecycleService.withdraw(memberId);
        response.addHeader("Set-Cookie", refreshTokenCookieManager.clearRefreshTokenCookie().toString());
        return ResponseEntity.ok(ApiResponse.success(MemberLifecycleService.WITHDRAW_COMPLETE_MESSAGE, null));
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
    public ResponseEntity<ApiResponse<PageResponse<PostCardResult>>> getMyPosts(
            @LoginMember Long memberId,
            @PageableDefault(size = 5, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PostCardResult> response = memberInfoService.getMyPosts(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }

    @Override
    @GetMapping("/me/comments")
    public ResponseEntity<ApiResponse<PageResponse<CommentItemResult>>> getMyComments(
            @LoginMember Long memberId,
            @PageableDefault(size = 5, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CommentItemResult> response = memberInfoService.getMyComments(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }

    @Override
    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<MyProfileResponse>> getUserProfileById(
            @PathVariable Long userId,
            @LoginMember(required = false) Long viewerId) {
        return ResponseEntity.ok(ApiResponse.success(memberInfoService.getUserProfile(userId, viewerId)));
    }

    @Override
    @GetMapping("/{userId}/photos")
    public ResponseEntity<ApiResponse<PageResponse<PhotoSummaryResponse>>> getUserPhotosById(
            @PathVariable Long userId,
            @LoginMember(required = false) Long viewerId,
            Pageable pageable) {
        Page<PhotoSummaryResponse> photos = memberInfoService.getUserPhotoList(userId, viewerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(photos)));
    }

    @Override
    @GetMapping("/{userId}/visits")
    public ResponseEntity<ApiResponse<PageResponse<VisitSummaryResponse>>> getUserVisitsById(
            @PathVariable Long userId,
            @LoginMember(required = false) Long viewerId,
            Pageable pageable) {
        Page<VisitSummaryResponse> page = memberInfoService.getUserVisits(userId, viewerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    @Override
    @GetMapping("/{userId}/posts")
    public ResponseEntity<ApiResponse<PageResponse<PostCardResult>>> getUserPostsById(
            @PathVariable Long userId,
            @LoginMember(required = false) Long viewerId,
            @PageableDefault(size = 5, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PostCardResult> response =
                memberInfoService.getUserPosts(userId, viewerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }

    @Override
    @GetMapping("/{userId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<CommentItemResult>>> getUserCommentsById(
            @PathVariable Long userId,
            @LoginMember(required = false) Long viewerId,
            @PageableDefault(size = 5, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CommentItemResult> response =
                memberInfoService.getUserComments(userId, viewerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }
}
