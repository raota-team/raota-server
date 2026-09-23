package com.raota.mobile.common.cursor;

import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * 정렬값과 ID로 다음 페이지의 시작 지점을 나타낸다. 형식은 {@code base64url(sortValue|id)}이며 패딩을 쓰지 않는다.
 * 커서는 조회 범위만 좁히며 접근 권한을 부여하지 않는다.
 *
 * <p>예: {@code ORDER BY created_at DESC, id DESC}로 정렬하고
 * {@code WHERE created_at < :sort OR (created_at = :sort AND id < :id)}와
 * {@code LIMIT :size + 1}로 조회한다.</p>
 */
public record Cursor(String sortValue, long id) {

    public Cursor {
        if (sortValue == null || sortValue.isEmpty() || id < 0) {
            throw invalidCursor();
        }
    }

    public static Cursor of(Instant sortValue, long id) {
        if (sortValue == null) {
            throw invalidCursor();
        }
        return new Cursor(sortValue.toString(), id);
    }

    public static Cursor of(long sortValue, long id) {
        return new Cursor(Long.toString(sortValue), id);
    }

    public String encode() {
        byte[] bytes = (sortValue + "|" + id).getBytes(StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** 요청에 커서가 없으면 첫 페이지를 뜻하는 빈 값을 반환한다. */
    public static Optional<Cursor> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        if (raw.length() > 200) {
            throw invalidCursor();
        }

        String decoded;
        try {
            decoded = new String(Base64.getUrlDecoder().decode(raw), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw invalidCursor();
        }

        int delimiter = decoded.lastIndexOf('|');
        if (delimiter <= 0) {
            throw invalidCursor();
        }

        try {
            return Optional.of(new Cursor(decoded.substring(0, delimiter),
                    Long.parseLong(decoded.substring(delimiter + 1))));
        } catch (NumberFormatException exception) {
            throw invalidCursor();
        }
    }

    private static MobileException invalidCursor() {
        return new MobileException(MobileErrorCode.VALIDATION_ERROR, "커서가 올바르지 않습니다.");
    }
}
