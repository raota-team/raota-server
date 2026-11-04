package com.raota.mobile.notification.infrastructure.external;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.mobile.push.expo")
public record ExpoPushProperties(
        @DefaultValue("https://exp.host") String baseUrl) {}
