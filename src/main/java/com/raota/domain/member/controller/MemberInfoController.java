package com.raota.domain.member.controller;

import com.raota.domain.member.controller.contract.MemberInfoApi;
import com.raota.domain.member.controller.request.UpdateProfileRequest;
import com.raota.domain.member.controller.response.BookmarkSummaryResponse;
import com.raota.domain.member.controller.response.MyCommentSummaryResponse;
import com.raota.domain.member.controller.response.MyPostSummaryResponse;
import com.raota.domain.member.controller.response.MyProfileResponse;
import com.raota.domain.member.controller.response.PhotoSummaryResponse;
import com.raota.domain.member.controller.response.VisitSummaryResponse;
import com.raota.domain.member.service.MemberInfoService;
import com.raota.global.auth.LoginMember;
import com.raota.global.common.ApiResponse;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
        MyProfileResponse updated = memberInfoService.updateMyProfile(request.getNickname(),
                request.getProfile_image_url(), memberId);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @Override
    @GetMapping("/me/photos")
    public ResponseEntity<ApiResponse<Page<PhotoSummaryResponse>>> getUserPhoto(
            @LoginMember Long memberId,
            Pageable pageable) {
        Page<PhotoSummaryResponse> photos = memberInfoService.getMyPhotoList(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(photos));
    }

    @Override
    @GetMapping("/me/bookmarks")
    public ResponseEntity<ApiResponse<Page<BookmarkSummaryResponse>>> getMyBookmarks(
            @LoginMember Long memberId,
            Pageable pageable) {
        Page<BookmarkSummaryResponse> page = memberInfoService.getMyBookmarks(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @Override
    @GetMapping("/me/visits")
    public ResponseEntity<ApiResponse<Page<VisitSummaryResponse>>> getMyVisits(
            @LoginMember Long memberId,
            Pageable pageable) {
        Page<VisitSummaryResponse> page = memberInfoService.getMyVisits(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @Override
    @GetMapping("/me/posts")
    public ResponseEntity<ApiResponse<Page<MyPostSummaryResponse>>> getMyPosts(
            @LoginMember Long memberId,
            @PageableDefault(size = 5, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(Page.empty(pageable)));
    }

    @Override
    @GetMapping("/me/comments")
    public ResponseEntity<ApiResponse<Page<MyCommentSummaryResponse>>> getMyComments(
            @LoginMember Long memberId,
            @PageableDefault(size = 5, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(Page.empty(pageable)));
    }
}
