package com.raota.presentation.api.member.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class UpdateProfileRequest {
    @Schema(description = "닉네임")
    private String nickname;
    @Schema(description = "프로필 이미지 URL")
    private String profile_image_url;
    @Schema(description = "백그라운드 이미지 URL")
    private String background_image_url;
    @Schema(description = "자기소개")
    private String bio;
}
