package com.raota.mobile.notification.presentation.request;

import com.raota.mobile.notification.domain.model.PushPlatform;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** {@code getExpoPushTokenAsync}가 주는 토큰이다. 예: {@code ExponentPushToken[xxxx]} */
public record MobilePushTokenRequest(
        @NotNull @Size(max = 255) @Pattern(regexp = "Expo(nent)?PushToken\\[[^\\]\\s]+]")
        String token,

        @NotNull PushPlatform platform) {}
