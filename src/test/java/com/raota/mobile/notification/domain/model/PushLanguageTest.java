package com.raota.mobile.notification.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PushLanguageTest {

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "ko-KR | KO",
                "en-US | EN",
                "ja-JP | JA",
                "zh-CN | ZH",
                "es-ES | ES",
                "fr-FR,es;q=0.8,en;q=0.5 | ES",
                "fr-FR | KO",
                "'' | KO",
                ";;;=== | KO"
            })
    void 앞선_언어부터_지원하는_언어를_고르고_없으면_한국어다(String header, PushLanguage expected) {
        assertThat(PushLanguage.fromAcceptLanguage(header)).isEqualTo(expected);
    }
}
