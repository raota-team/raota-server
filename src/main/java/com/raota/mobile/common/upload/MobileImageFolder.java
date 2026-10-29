package com.raota.mobile.common.upload;

/**
 * 앱이 올리는 사진의 저장소 폴더다. 업로드 티켓(어디에 올릴지), 기록 사진 검증(어떤 주소를 받을지), 고아 사진 정리(어느 폴더를
 * 볼지)가 모두 이 값을 쓴다. 한쪽만 다른 값을 쓰면 정리 작업이 살아 있는 사진을 지운다.
 */
public enum MobileImageFolder {
    RAMEN_LOG("v2/ramen-logs/"),
    PROFILE("v2/profiles/");

    private final String prefix;

    MobileImageFolder(String prefix) {
        this.prefix = prefix;
    }

    /** 객체 키 접두어다. {@code /}로 끝난다. */
    public String prefix() {
        return prefix;
    }

    /** 업로드 서명을 만들 때 넘기는 디렉터리다. 끝 {@code /}가 없다. */
    public String directory() {
        return prefix.substring(0, prefix.length() - 1);
    }
}
