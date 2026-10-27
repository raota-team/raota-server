package com.raota.global.file.cleanup;

import java.util.Collection;
import java.util.Set;

/**
 * 업로드 사진을 소유한 모듈이 제공한다. 고아 사진 정리({@link OrphanImageCleaner})는 {@link #prefix()} 아래 객체 중 이
 * 모듈이 참조한다고 답하지 않은 것만 지운다.
 */
public interface StoredImageReferences {

    /** 담당하는 객체 키 접두어. {@code v2/}로 시작하고 {@code /}로 끝나야 한다. */
    String prefix();

    /**
     * 주어진 CDN 주소 중 DB가 참조하는 것. 지운 기록처럼 아직 행이 남아 있으면 참조로 본다. 확인할 수 없으면 예외를 던진다(그 접두어는
     * 건너뛴다).
     */
    Set<String> referencedUrls(Collection<String> urls);
}
