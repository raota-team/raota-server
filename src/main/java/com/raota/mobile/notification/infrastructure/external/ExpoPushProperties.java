package com.raota.mobile.notification.infrastructure.external;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.mobile.push.expo")
public record ExpoPushProperties(
        @DefaultValue("https://exp.host") String baseUrl,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("10s") Duration readTimeout) {}
