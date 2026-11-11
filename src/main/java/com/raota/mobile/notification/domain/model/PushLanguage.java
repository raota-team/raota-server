package com.raota.mobile.notification.domain.model;

import java.util.List;
import java.util.Locale;

/** 앱 {@code SUPPORTED_LOCALES}와 같은 5개 언어다. */
public enum PushLanguage {
    KO(Locale.KOREA),
    EN(Locale.US),
    JA(Locale.JAPAN),
    ZH(Locale.SIMPLIFIED_CHINESE),
    ES(Locale.forLanguageTag("es-ES"));

    private final Locale locale;

    PushLanguage(Locale locale) {
        this.locale = locale;
    }

    public Locale locale() {
        return locale;
    }

    /** 헤더가 없거나 읽을 수 없거나 지원하는 언어가 없으면 앱 기본값인 한국어다. */
    public static PushLanguage fromAcceptLanguage(String header) {
        if (header == null || header.isBlank()) {
            return KO;
        }
        List<Locale.LanguageRange> ranges;
        try {
            ranges = Locale.LanguageRange.parse(header);
        } catch (IllegalArgumentException exception) {
            return KO;
        }
        for (Locale.LanguageRange range : ranges) {
            String language = Locale.forLanguageTag(range.getRange()).getLanguage();
            for (PushLanguage candidate : values()) {
                if (candidate.locale.getLanguage().equals(language)) {
                    return candidate;
                }
            }
        }
        return KO;
    }
}
