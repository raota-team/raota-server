package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.ramenlog.application.config.MobileUploadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 앱에 발급한 저장소 주소만 기록 사진으로 허용한다. */
@Component
@RequiredArgsConstructor
public class MobileUploadedImagePolicy {

    private final MobileUploadProperties properties;

    public boolean isOwnImage(String url) {
        return url.startsWith(properties.imageBaseUrl());
    }

}
