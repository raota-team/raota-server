package com.raota.mobile.ramenlog.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MobileCreateCommentRequest(@NotBlank @Size(min = 2, max = 500) String content) {
}
