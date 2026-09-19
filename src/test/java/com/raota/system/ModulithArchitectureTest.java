package com.raota.system;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.raota.RaotaApplication;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.raota");

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

        assertEquals(
                Set.of(
                        "global",
                        "agent",
                        "web.account",
                        "web.community",
                        "web.ramenlog",
                        "web.ramenshop"
                ),
                modules.stream()
                        .map(module -> module.getIdentifier().toString())
                        .collect(Collectors.toSet())
        );

        modules
                .verify();
    }

    @Test
    void webDoesNotDependOnMobile() {
        noClasses().that().resideInAPackage("com.raota.web..")
                .should().dependOnClassesThat().resideInAPackage("com.raota.mobile..")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void mobileDoesNotDependOnWeb() {
        noClasses().that().resideInAPackage("com.raota.mobile..")
                .should().dependOnClassesThat().resideInAPackage("com.raota.web..")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }
}
