package com.raota.mobile.common.cursor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CursorPageTest {

    @Test
    void 추가_항목이_있으면_포함된_마지막_항목의_커서를_만든다() {
        CursorPage<Integer> page = CursorPage.of(List.of(1, 2, 3), 2, CursorPageTest::cursorOf);

        assertThat(page.items()).containsExactly(1, 2);
        assertThat(page.hasNext()).isTrue();
        assertThat(Cursor.parse(page.nextCursor())).contains(cursorOf(2));
    }

    @Test
    void 가져온_항목이_크기와_같으면_다음_페이지가_없다() {
        CursorPage<Integer> page = CursorPage.of(List.of(1, 2), 2, CursorPageTest::cursorOf);

        assertThat(page.items()).containsExactly(1, 2);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void 빈_목록에도_다음_페이지가_없다() {
        CursorPage<Integer> page = CursorPage.of(List.<Integer>of(), 2, CursorPageTest::cursorOf);

        assertThat(page.items()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void 결과_목록은_원본_변경과_결과_수정을_막는다() {
        List<Integer> fetched = new ArrayList<>(List.of(1, 2, 3));
        CursorPage<Integer> page = CursorPage.of(fetched, 2, CursorPageTest::cursorOf);

        fetched.set(0, 9);

        assertThat(page.items()).containsExactly(1, 2);
        assertThatThrownBy(() -> page.items().add(4)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void 크기가_0이면_프로그래밍_오류다() {
        assertThatThrownBy(() -> CursorPage.of(List.of(1), 0, CursorPageTest::cursorOf))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Cursor cursorOf(int item) {
        return Cursor.of(item, item);
    }
}
