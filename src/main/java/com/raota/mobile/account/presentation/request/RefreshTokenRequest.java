package com.raota.mobile.account.presentation.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 소비하거나 폐기할 기기의 리프레시 토큰이다. 필드 이름이 틀리면 값이 비어 로그아웃이 아무것도 폐기하지 않은 채 204를 돌려주므로 400으로
 * 막는다.
 */
public record RefreshTokenRequest(@NotBlank String refreshToken) {}
