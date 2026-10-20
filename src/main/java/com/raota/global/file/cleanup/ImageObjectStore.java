package com.raota.global.file.cleanup;

import java.time.Instant;
import java.util.List;

/** 고아 사진 정리가 쓰는 사진 저장소. 운영 저장소(prod)에만 구현이 있고, 없으면 정리하지 않는다. */
public interface ImageObjectStore {

    /** 접두어 아래에서 마지막 수정 시각이 {@code cutoff}보다 이른 객체. */
    List<StoredImage> listOlderThan(String prefix, Instant cutoff);

    void delete(String key);

    /**
     * @param url 앱과 DB가 쓰는 CDN 주소
     */
    record StoredImage(String key, String url, Instant lastModified) {
    }

}
