package com.raota.account.presentation.member.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateEmailRequest(
        @Schema(description = "대표 이메일", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email
) {
}
