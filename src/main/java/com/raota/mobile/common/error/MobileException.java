package com.raota.mobile.common.error;

import java.util.Objects;

public final class MobileException extends RuntimeException {

    private final MobileErrorCode code;

    public MobileException(MobileErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code);
    }

    public MobileErrorCode code() {
        return code;
    }
}
