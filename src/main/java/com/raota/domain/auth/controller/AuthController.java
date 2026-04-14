package com.raota.domain.auth.controller;

import com.raota.domain.auth.controller.response.AuthTokenResponse;
import com.raota.domain.auth.service.AuthService;
import com.raota.domain.auth.service.TokenRefreshResult;
import com.raota.global.auth.RefreshTokenCookieManager;
import com.raota.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = refreshTokenCookieManager.extractRefreshToken(request);
        TokenRefreshResult result = authService.refresh(refreshToken);
        response.addHeader("Set-Cookie", refreshTokenCookieManager.createRefreshTokenCookie(result.refreshToken()).toString());
        return ResponseEntity.ok(ApiResponse.success(AuthTokenResponse.bearer(
                result.accessToken(),
                result.accessTokenExpiresIn(),
                result.memberId()
        )));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = refreshTokenCookieManager.extractRefreshToken(request);
        authService.logout(refreshToken);
        response.addHeader("Set-Cookie", refreshTokenCookieManager.clearRefreshTokenCookie().toString());
        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다.", null));
    }
}
