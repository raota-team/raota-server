package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.common.upload.MobileImageFolder;
import com.raota.mobile.ramenlog.application.config.MobileUploadProperties;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 앱에 발급한 기록 사진 주소만 허용한다. 프로필 사진처럼 다른 폴더의 주소를 받으면, 그 폴더의 정리 작업은 기록이 쓰는 줄 모르고 7일 뒤
 * 사진을 지운다.
 */
@Component
@RequiredArgsConstructor
public class MobileUploadedImagePolicy {

    /** 업로드 티켓이 만드는 파일 이름(UUID와 확장자)이다. 하위 경로, {@code ..}, 인코딩된 문자, 쿼리로 다른 객체를 가리키지 못하게 한다. */
    private static final Pattern FILE_NAME = Pattern.compile("[A-Za-z0-9_-]+\\.[A-Za-z0-9]+");

    private final MobileUploadProperties uploadProperties;

    public boolean isRamenLogImage(String url) {
        String folderUrl = uploadProperties.imageBaseUrl() + MobileImageFolder.RAMEN_LOG.prefix();
        if (!url.startsWith(folderUrl)) {
            return false;
        }
        return FILE_NAME.matcher(url.substring(folderUrl.length())).matches();
    }
}
