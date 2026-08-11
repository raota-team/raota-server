package com.raota.account.presentation.member.request;

import jakarta.validation.constraints.NotNull;

public record ActivityVisibilityUpdateRequest(
        @NotNull Boolean logs,
        @NotNull Boolean visits,
        @NotNull Boolean posts,
        @NotNull Boolean comments
) {
}
