package com.raota.mobile.account.domain.model;

import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** 표시용 닉네임과 대소문자를 구별하지 않는 중복 판별값을 함께 보관한다. */
public record Nickname(String display, String normalized) {

    private static final Pattern ALLOWED = Pattern.compile("[가-힣a-zA-Z0-9_]+");

    public static Nickname of(String raw) {
        String display = raw == null ? "" : Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC);
        int length = display.codePointCount(0, display.length());
        if (length < 2 || length > 12 || !ALLOWED.matcher(display).matches()) {
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "닉네임은 2~12자의 한글·영문·숫자·밑줄만 쓸 수 있습니다.");
        }
        return new Nickname(display, display.toLowerCase(Locale.ROOT));
    }

}
