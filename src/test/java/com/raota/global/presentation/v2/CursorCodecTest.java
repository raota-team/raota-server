package com.raota.global.presentation.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CursorCodecTest {

    @Test
    @DisplayName("커서를 인코딩한 뒤 디코딩하면 정렬값과 id가 그대로 나온다.")
    void encodeThenDecodeKeepsValues() {
        String cursor = CursorCodec.encode("2026-09-22T01:02:03Z", "1234");

        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);

        assertThat(decoded.sortValue()).isEqualTo("2026-09-22T01:02:03Z");
        assertThat(decoded.id()).isEqualTo("1234");
    }

    @Test
    @DisplayName("정렬값에 구분자가 들어 있어도 id를 잘못 자르지 않는다.")
    void decodeUsesLastDelimiter() {
        String raw = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("가게|이름|99".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        CursorCodec.Cursor decoded = CursorCodec.decode(raw);

        assertThat(decoded.sortValue()).isEqualTo("가게|이름");
        assertThat(decoded.id()).isEqualTo("99");
    }

    @Test
    @DisplayName("빈 값·잘못된 형식·id 없는 커서는 InvalidCursorException이다.")
    void invalidCursorsAreRejected() {
        assertThatThrownBy(() -> CursorCodec.decode(null)).isInstanceOf(InvalidCursorException.class);
        assertThatThrownBy(() -> CursorCodec.decode("   ")).isInstanceOf(InvalidCursorException.class);
        assertThatThrownBy(() -> CursorCodec.decode("!!not-base64!!")).isInstanceOf(InvalidCursorException.class);

        String withoutId = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("정렬값만".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThatThrownBy(() -> CursorCodec.decode(withoutId)).isInstanceOf(InvalidCursorException.class);
    }

    @Test
    @DisplayName("정렬값에 구분자를 넣어 인코딩하면 거부한다.")
    void encodeRejectsDelimiterInSortValue() {
        assertThatThrownBy(() -> CursorCodec.encode("a|b", "1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
