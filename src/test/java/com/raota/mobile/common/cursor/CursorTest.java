package com.raota.mobile.common.cursor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class CursorTest {

    @Test
    void 마이크로초_시각을_인코딩하고_복원한다() {
        Instant sortValue = Instant.parse("2026-09-23T02:00:00.123456Z");
        Cursor cursor = Cursor.of(sortValue, 42);

        Cursor restored = Cursor.parse(cursor.encode()).orElseThrow();

        assertThat(restored).isEqualTo(cursor);
        assertThat(restored.sortValue()).isEqualTo("2026-09-23T02:00:00.123456Z");
    }

    @Test
    void 숫자_정렬값을_인코딩하고_복원한다() {
        Cursor cursor = Cursor.of(123456789L, 42L);

        Cursor restored = Cursor.parse(cursor.encode()).orElseThrow();

        assertThat(restored).isEqualTo(cursor);
        assertThat(restored.sortValue()).isEqualTo("123456789");
    }

    @Test
    void 인코딩한_커서에는_패딩이나_URL에_안전하지_않은_문자가_없다() {
        String encoded = Cursor.of(Instant.parse("2026-09-23T02:00:00.123456Z"), 42).encode();

        assertThat(encoded).doesNotContain("+", "/", "=");
    }

    @Test
    void 커서가_없거나_공백이면_첫_페이지로_해석한다() {
        assertThat(Cursor.parse(null)).isEmpty();
        assertThat(Cursor.parse(" \t\n ")).isEmpty();
    }

    @Test
    void 잘못된_Base64URL은_검증_오류다() {
        assertInvalid(() -> Cursor.parse("!!!"));
    }

    @Test
    void 구분자가_없는_커서는_검증_오류다() {
        assertInvalid(() -> Cursor.parse(encodedPayload("sort42")));
    }

    @Test
    void 정렬값이_비어_있는_커서는_검증_오류다() {
        assertInvalid(() -> Cursor.parse(encodedPayload("|42")));
    }

    @Test
    void 숫자가_아닌_ID는_검증_오류다() {
        assertInvalid(() -> Cursor.parse(encodedPayload("sort|abc")));
    }

    @Test
    void 음수_ID는_검증_오류다() {
        assertInvalid(() -> Cursor.parse(encodedPayload("sort|-1")));
    }

    @Test
    void 너무_긴_커서는_검증_오류다() {
        assertInvalid(() -> Cursor.parse("a".repeat(201)));
    }

    @Test
    void 구분자가_정렬값에_있으면_마지막_구분자를_사용한다() {
        Cursor cursor = new Cursor("part|sort", 42L);

        assertThat(Cursor.parse(cursor.encode())).contains(cursor);
    }

    @Test
    void 생성자의_null이나_빈_정렬값과_음수_ID는_검증_오류다() {
        assertInvalid(() -> new Cursor(null, 1L));
        assertInvalid(() -> new Cursor("", 1L));
        assertInvalid(() -> Cursor.of(10L, -1L));
        assertInvalid(() -> Cursor.of((Instant) null, 1L));
    }

    private static String encodedPayload(String payload) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertInvalid(ThrowingCallable action) {
        assertThatExceptionOfType(MobileException.class)
                .isThrownBy(action)
                .satisfies(exception -> {
                    assertThat(exception.code()).isEqualTo(MobileErrorCode.VALIDATION_ERROR);
                    assertThat(exception.getMessage()).isEqualTo("커서가 올바르지 않습니다.");
                });
    }
}
