package com.raota.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external-api.kma")
public record KmaProperties(
    String authKey,
    String serviceKey,
    String baseUrl,
    String stnId
) {

    public String resolvedAuthKey() {
        return authKey != null && !authKey.isBlank() ? authKey : serviceKey;
    }
}
