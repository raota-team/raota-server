package com.raota.infrastructure.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@Profile("prod")
public class ObjectStorageConfig {
    @Value("${oci.storage.namespace}")
    private String namespace;

    @Value("${oci.storage.region}")
    private String region;

    @Value("${oci.storage.access-key}")
    private String accessKey;

    @Value("${oci.storage.secret-key}")
    private String secretKey;

    @Bean
    public S3Presigner s3Presigner(){
        return S3Presigner.builder()
                .endpointOverride(URI.create(compatEndpoint()))
                .region(Region.of(required("oci.storage.region", region)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .credentialsProvider(credentialsProvider())
                .build();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(compatEndpoint()))
                .region(Region.of(required("oci.storage.region", region)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .credentialsProvider(credentialsProvider())
                .build();
    }

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                        required("oci.storage.access-key", accessKey),
                        required("oci.storage.secret-key", secretKey)
                )
        );
    }

    private String compatEndpoint() {
        return String.format(
                "https://%s.compat.objectstorage.%s.oraclecloud.com",
                required("oci.storage.namespace", namespace),
                required("oci.storage.region", region)
        );
    }

    private String required(String propertyName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(propertyName + " must not be blank");
        }
        return value.trim();
    }
}
