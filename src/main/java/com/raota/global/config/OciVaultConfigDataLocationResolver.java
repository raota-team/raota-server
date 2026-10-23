package com.raota.global.config;

import java.util.List;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;

/**
 * {@code oci-vault:<region>/<vault-id>} import를 읽는다. {@code app.oci-vault.enabled=false}면
 * Vault를 읽지 않는다. OCI 인증이 없는 CI에서 운영 설정으로 서버를 띄워 볼 때 쓴다.
 */
public class OciVaultConfigDataLocationResolver implements ConfigDataLocationResolver<OciVaultConfigDataResource> {

    private static final String PREFIX = "oci-vault:";

    static final String ENABLED_PROPERTY = "app.oci-vault.enabled";

    @Override
    public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
        return location.hasPrefix(PREFIX);
    }

    @Override
    public List<OciVaultConfigDataResource> resolve(ConfigDataLocationResolverContext context,
            ConfigDataLocation location) {
        if (!context.getBinder().bind(ENABLED_PROPERTY, Boolean.class).orElse(true)) {
            return List.of();
        }
        String value = location.getNonPrefixedValue(PREFIX);
        int delimiterIndex = value.indexOf('/');

        if (delimiterIndex <= 0 || delimiterIndex == value.length() - 1) {
            throw new IllegalArgumentException(
                    "OCI Vault config import must use the format oci-vault:<region>/<vault-id>");
        }

        String region = value.substring(0, delimiterIndex);
        String vaultId = value.substring(delimiterIndex + 1);

        return List.of(new OciVaultConfigDataResource(location.isOptional(), region, vaultId));
    }

}
