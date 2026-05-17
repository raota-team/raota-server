package com.raota.presentation.common;

public enum ApiStatus {

    SUCCESS(true),
    FAIL(false);

    private final boolean status;

    ApiStatus(boolean status) {
        this.status = status;
    }

}
