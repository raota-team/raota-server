package com.raota.mobile.account.presentation;

import com.raota.mobile.account.application.result.MobileLoginResult;
import com.raota.mobile.account.application.service.MobileAuthService;
import com.raota.mobile.account.presentation.request.SocialLoginRequest;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 모바일 앱의 소셜 인증 진입점이다. */
@RestController
@RequestMapping("/api/v2/auth")
public class MobileAuthController {

    private final MobileAuthService authService;

    public MobileAuthController(MobileAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/oauth/login")
    public MobileApiResponse<MobileLoginResult> login(@Valid @RequestBody SocialLoginRequest request) {
        return MobileApiResponse.success(authService.login(request.toCommand()));
    }

}
