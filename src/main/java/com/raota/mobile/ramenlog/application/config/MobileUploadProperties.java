package com.raota.mobile.ramenlog.application.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 사진 업로드 후 앱에 반환하는 공개 저장소의 주소다. */
@ConfigurationProperties("app.mobile.upload")
@Validated
public record MobileUploadProperties(@NotBlank String imageBaseUrl) {
}
