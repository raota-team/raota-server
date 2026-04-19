package com.raota.global.config;

import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.secrets.SecretsClient;
import com.oracle.bmc.secrets.model.Base64SecretBundleContentDetails;
import com.oracle.bmc.secrets.requests.GetSecretBundleByNameRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public class OciVaultEnvironmentPostProcessor implements EnvironmentPostProcessor {
    private static final String PROPERTY_SOURCE_NAME = "ociVaultSecrets";

    private static final List<String> SECRET_NAMES = List.of(
            "SPRING_DATASOURCE_URL",
            "SPRING_DATASOURCE_USERNAME",
            "SPRING_DATASOURCE_PASSWORD",
            "SPRING_DATA_REDIS_HOST",
            "SPRING_DATA_REDIS_PORT",
            "OCI_STORAGE_NAMESPACE",
            "OCI_STORAGE_BUCKET",
            "OCI_STORAGE_REGION",
            "OCI_STORAGE_ACCESS_KEY",
            "OCI_STORAGE_SECRET_KEY",
            "GOOGLE_CLIENT_ID",
            "GOOGLE_CLIENT_SECRET",
            "KAKAO_CLIENT_ID",
            "KAKAO_CLIENT_SECRET",
            "APP_AUTH_ACCESS_TOKEN_SECRET",
            "APP_AUTH_OAUTH2_REDIRECT_URI",
            "APP_AUTH_OAUTH2_FAILURE_REDIRECT_URI",
            "APP_AUTH_ALLOWED_ORIGINS",
            "DISCORD_WEBHOOK_URL"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Binder binder = Binder.get(environment);
        boolean enabled = binder.bind("oci.vault.enabled", Boolean.class).orElse(false);

        if (!enabled) {
            return;
        }

        String region = binder.bind("oci.vault.region", String.class).orElse(null);
        String vaultId = binder.bind("oci.vault.vault-id", String.class).orElse(null);

        if (StringUtils.hasText(region) && StringUtils.hasText(vaultId)) {
            try {
                Map<String, Object> resolvedSecrets = loadSecrets(region, vaultId);
                if (!resolvedSecrets.isEmpty()) {
                    environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, resolvedSecrets));
                }
            } catch (Exception e) {
                // 초기 단계이므로 System.err 사용 (로깅 시스템 미준비 상태일 수 있음)
                System.err.println("[OCI-VAULT] Critical error loading secrets from OCI Vault: " + e.getMessage());
            }
        }
    }

    private Map<String, Object> loadSecrets(String region, String vaultId) {
        InstancePrincipalsAuthenticationDetailsProvider provider =
                InstancePrincipalsAuthenticationDetailsProvider.builder().build();

        try (SecretsClient secretsClient = new SecretsClient(provider)) {
            secretsClient.setRegion(region);

            Map<String, Object> resolvedSecrets = new LinkedHashMap<>();
            for (String secretName : SECRET_NAMES) {
                try {
                    resolvedSecrets.put(secretName, fetchSecretValue(secretsClient, vaultId, secretName));
                } catch (Exception e) {
                    System.err.println("[OCI-VAULT] Warning: Failed to fetch secret: " + secretName + " - " + e.getMessage());
                }
            }
            return resolvedSecrets;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize SecretsClient for OCI Vault", exception);
        }
    }

    private String fetchSecretValue(SecretsClient secretsClient, String vaultId, String secretName) {
        var response = secretsClient.getSecretBundleByName(GetSecretBundleByNameRequest.builder()
                .vaultId(vaultId)
                .secretName(secretName)
                .stage(GetSecretBundleByNameRequest.Stage.Current)
                .build());

        var content = response.getSecretBundle().getSecretBundleContent();
        if (!(content instanceof Base64SecretBundleContentDetails base64Content)) {
            throw new IllegalStateException("Unsupported OCI secret content type for secret: " + secretName);
        }

        return new String(Base64.getDecoder().decode(base64Content.getContent()), StandardCharsets.UTF_8);
    }
}
