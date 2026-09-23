package com.raota.mobile.common.presentation.response;

import java.util.List;

public record MobileError(String code, String message, List<MobileFieldError> fields) {
    public static MobileError of(MobileErrorCode code, String message) {
        return new MobileError(code.name(), message, List.of());
    }

    public static MobileError of(MobileErrorCode code, String message, List<MobileFieldError> fields) {
        return new MobileError(code.name(), message, List.copyOf(fields));
    }
}
