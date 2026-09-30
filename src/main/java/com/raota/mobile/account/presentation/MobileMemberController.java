package com.raota.mobile.account.presentation;

import com.raota.mobile.account.application.result.MobileMemberResult;
import com.raota.mobile.account.application.result.MobileNicknameAvailabilityResult;
import com.raota.mobile.account.application.service.MobileMemberService;
import com.raota.mobile.account.presentation.request.MobileOnboardingRequest;
import com.raota.mobile.account.presentation.request.MobileProfileUpdateRequest;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 모바일 회원의 내 정보 조회와 수정 진입점이다. */
@RestController
@RequestMapping("/api/v2/members")
@RequiredArgsConstructor
public class MobileMemberController {

    private final MobileMemberService members;

    @GetMapping("/me")
    public MobileApiResponse<MobileMemberResult> me(@LoginUser Long userId) {
        return MobileApiResponse.success(members.me(userId));
    }

    @PatchMapping("/me")
    public MobileApiResponse<MobileMemberResult> updateProfile(@LoginUser Long userId,
            @Valid @RequestBody MobileProfileUpdateRequest request) {
        return MobileApiResponse.success(members.updateProfile(userId, request.email(), request.avatarUrl(),
                request.bio(), request.favoriteRamenType()));
    }

    @GetMapping("/nickname-availability")
    public MobileApiResponse<MobileNicknameAvailabilityResult> nicknameAvailability(@LoginUser Long userId,
            @RequestParam String nickname) {
        return MobileApiResponse.success(members.nicknameAvailability(userId, nickname));
    }

    @PutMapping("/me/onboarding")
    public MobileApiResponse<MobileMemberResult> onboard(@LoginUser Long userId,
            @Valid @RequestBody MobileOnboardingRequest request) {
        return MobileApiResponse.success(members.onboard(userId, request.toCommand()));
    }

}
