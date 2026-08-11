package com.raota.system;

import com.raota.RaotaApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTest {

    @Test
    void printModules() {
        var modules = ApplicationModules.of(RaotaApplication.class);

        modules.forEach(module ->
                System.out.println("MODULE = " + module.getIdentifier())
        );
    }

    @Test
    void verifyModules() {
        ApplicationModules.of(RaotaApplication.class)
                .verify();
    }
}