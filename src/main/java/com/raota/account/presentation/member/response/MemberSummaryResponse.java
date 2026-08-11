package com.raota.account.presentation.member.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record MemberSummaryResponse(
        @Schema(description = "사용자 ID")
        Long id,
        @Schema(description = "닉네임")
        String nickname,
        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl
) {
}
