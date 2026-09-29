package com.raota.mobile.account.presentation.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 누락한 필드는 유지하고 빈 문자열은 지우는 프로필 변경 요청이다. */
public record MobileProfileUpdateRequest(@Email @Size(max = 255) String email,
        @Size(max = 1000) @Pattern(regexp = "^$|^https://[^\\s/?#]+(?:[/?#][^\\s]*)?$",
                message = "https URL이어야 합니다.") String avatarUrl,
        @Size(max = 500) String bio, @Size(max = 50) String favoriteRamenType) {
}
