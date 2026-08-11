package com.raota.system;

import com.raota.RaotaApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import java.util.Set;

class ModulithArchitectureTest {

    @Test
    void printModules() {

        ApplicationModules.of(RaotaApplication.class)
                .forEach(module ->
                        System.out.println(
                                module.getIdentifier()
                                        + " : "
                                        + module.getBasePackage()
                        )
                );
    }

    @Test
    void verifyModules() {
        var modules = ApplicationModules.of(RaotaApplication.class);

        org.junit.jupiter.api.Assertions.assertEquals(
                Set.of("global", "ramenlog", "account"),
                modules.stream()
                        .map(module -> module.getIdentifier().toString())
                        .collect(java.util.stream.Collectors.toSet())
        );

        modules
                .verify();
    }
}
