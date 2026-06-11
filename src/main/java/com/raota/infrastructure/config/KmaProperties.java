package com.raota.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external-api.kma")
public record KmaProperties(
    String serviceKey,
    String baseUrl,
    String stnId
) {
}
