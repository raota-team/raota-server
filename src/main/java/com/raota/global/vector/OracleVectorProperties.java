package com.raota.global.vector;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.oracle")
public record OracleVectorProperties(
        String url,
        String username,
        String password,
        String driverClassName,
        boolean initializeSchema,
        String indexType,
        String distanceType,
        int dimensions,
        boolean forcedNormalization,
        boolean removeExistingVectorStoreTable,
        int searchAccuracy
) {}
