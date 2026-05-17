package com.raota.presentation.api.auth;

import com.raota.presentation.api.auth.dto.AuthTokenResponse;
import com.raota.application.auth.AuthService;
import com.raota.application.auth.TokenRefreshResult;
import com.raota.infrastructure.auth.RefreshTokenCookieManager;
import com.raota.presentation.common.ApiResponse;
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
