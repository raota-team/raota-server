package com.raota.global.config;

import java.util.List;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;

public class OciVaultConfigDataLocationResolver implements ConfigDataLocationResolver<OciVaultConfigDataResource> {

    private static final String PREFIX = "oci-vault:";

    @Override
    public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
        return location.hasPrefix(PREFIX);
    }

    @Override
    public List<OciVaultConfigDataResource> resolve(
            ConfigDataLocationResolverContext context,
            ConfigDataLocation location
    ) {
        String value = location.getNonPrefixedValue(PREFIX);
        int delimiterIndex = value.indexOf('/');

        if (delimiterIndex <= 0 || delimiterIndex == value.length() - 1) {
            throw new IllegalArgumentException(
                    "OCI Vault config import must use the format oci-vault:<region>/<vault-id>"
            );
        }

        String region = value.substring(0, delimiterIndex);
        String vaultId = value.substring(delimiterIndex + 1);

        return List.of(new OciVaultConfigDataResource(location.isOptional(), region, vaultId));
    }
}
