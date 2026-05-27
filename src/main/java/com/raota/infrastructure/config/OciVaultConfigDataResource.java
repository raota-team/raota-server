package com.raota.infrastructure.config;

import org.springframework.boot.context.config.ConfigDataResource;

public final class OciVaultConfigDataResource extends ConfigDataResource {

    private final String region;
    private final String vaultId;

    public OciVaultConfigDataResource(boolean optional, String region, String vaultId) {
        super(optional);
        this.region = region;
        this.vaultId = vaultId;
    }

    public String region() {
        return region;
    }

    public String vaultId() {
        return vaultId;
    }
}
