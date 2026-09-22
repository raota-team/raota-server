package com.raota.global.presentation.v2;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * keyset 페이지네이션 커서를 인코딩·디코딩한다.
 *
 * <p>커서는 {@code base64url(정렬값 + '|' + id)}다. 정렬값이 같은 행이 있을 수 있으므로 id를
 * tie-breaker로 항상 함께 담는다. 서명·만료·저장소는 사용하지 않는다. 커서는 비밀이 아니라
 * "어디까지 봤는지"를 나타내는 값이며, 서버는 값을 그대로 믿지 않고 조회 조건에만 사용한다.</p>
 */
public final class CursorCodec {

    private static final String DELIMITER = "|";

    private CursorCodec() {
    }

    public static String encode(String sortValue, String id) {
        if (sortValue == null || id == null) {
            throw new IllegalArgumentException("커서의 정렬값과 id는 필수입니다.");
        }
        if (sortValue.contains(DELIMITER)) {
            throw new IllegalArgumentException("커서 정렬값에 구분자를 넣을 수 없습니다.");
        }
        String raw = sortValue + DELIMITER + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            throw new InvalidCursorException("커서가 비어 있습니다.");
        }
        String raw;
        try {
            raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new InvalidCursorException("커서 형식이 올바르지 않습니다.");
        }
        int separator = raw.lastIndexOf(DELIMITER);
        if (separator <= 0 || separator == raw.length() - 1) {
            throw new InvalidCursorException("커서 형식이 올바르지 않습니다.");
        }
        return new Cursor(raw.substring(0, separator), raw.substring(separator + 1));
    }

    public record Cursor(String sortValue, String id) {
    }
}
