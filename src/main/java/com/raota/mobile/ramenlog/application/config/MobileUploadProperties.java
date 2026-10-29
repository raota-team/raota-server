package com.raota.mobile.ramenlog.application.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 사진 업로드 후 앱에 반환하는 공개 저장소의 주소다. 끝 {@code /}가 없으면 {@code https://images.raota.net.evil.com/}처럼
 * 호스트만 앞이 같은 주소도 통과하므로, 값이 틀리면 서버가 뜨지 않게 한다.
 */
@ConfigurationProperties("app.mobile.upload")
@Validated
public record MobileUploadProperties(
        @NotBlank @Pattern(regexp = "https?://[^/]+/(.*/)?", message = "http(s)://호스트/ 형식이고 /로 끝나야 합니다.")
        String imageBaseUrl) {}
