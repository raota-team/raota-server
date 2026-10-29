package com.raota.mobile.ramenlog.application.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MobileUploadPropertiesTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @ValueSource(strings = {"https://images.raota.net/", "https://mock.cdn.com/", "http://localhost:8080/files/"})
    void 저장소_주소는_슬래시로_끝나야_받는다(String imageBaseUrl) {
        assertThat(validator.validate(new MobileUploadProperties(imageBaseUrl))).isEmpty();
    }

    // 끝 / 가 없으면 https://images.raota.net.evil.com/... 처럼 호스트 앞부분만 같은 주소가 기록 사진으로 통과한다.
    @ParameterizedTest
    @ValueSource(strings = {"https://images.raota.net", "https://images.raota.net/v2", "images.raota.net/", " "})
    void 슬래시로_끝나지_않거나_주소가_아니면_거절한다(String imageBaseUrl) {
        assertThat(validator.validate(new MobileUploadProperties(imageBaseUrl))).isNotEmpty();
    }
}
