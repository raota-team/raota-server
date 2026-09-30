package com.raota.system;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.raota.RaotaApplication;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.beans.Introspector;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.stereotype.Component;

class ModulithArchitectureTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.raota");

    @Test
    void printModules() {

        ApplicationModules.of(RaotaApplication.class)
            .forEach(module -> System.out.println(module.getIdentifier() + " : " + module.getBasePackage()));
    }

    @Test
    void verifyModules() {
        var modules = ApplicationModules.of(RaotaApplication.class);

        assertEquals(
                Set.of("global", "agent", "web.account", "web.community", "web.ramenlog", "web.ramenshop",
                        "mobile.account", "mobile.common"),
                modules.stream().map(module -> module.getIdentifier().toString()).collect(Collectors.toSet()));

        modules.verify();
    }

    @Test
    void webDoesNotDependOnMobile() {
        noClasses().that()
            .resideInAPackage("com.raota.web..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.raota.mobile..")
            .allowEmptyShould(true)
            .check(PRODUCTION_CLASSES);
    }

    @Test
    void mobileDoesNotDependOnWeb() {
        noClasses().that()
            .resideInAPackage("com.raota.mobile..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.raota.web..")
            .allowEmptyShould(true)
            .check(PRODUCTION_CLASSES);
    }

    @Test
    void mobileCommonDoesNotDependOnOtherMobileModules() {
        noClasses().that()
            .resideInAPackage("com.raota.mobile.common..")
            .should()
            .dependOnClassesThat(
                    resideInAPackage("com.raota.mobile..").and(not(resideInAPackage("com.raota.mobile.common.."))))
            .allowEmptyShould(true)
            .check(PRODUCTION_CLASSES);
    }

    /**
     * 컴포넌트 스캔 빈 이름이 겹치면 서버가 시작하지 못한다. 조건부 빈은 테스트 프로필에서 만들어지지 않을 수 있어서 설정과 관계없이 클래스만 보고
     * 검사한다.
     */
    @Test
    void componentBeanNamesAreUnique() {
        Map<String, List<String>> classesByBeanName = PRODUCTION_CLASSES.stream()
            .filter(javaClass -> !javaClass.isInterface() && !javaClass.isAnnotation())
            .filter(javaClass -> javaClass.isMetaAnnotatedWith(Component.class)
                    || javaClass.isAnnotatedWith(Component.class))
            .collect(Collectors.groupingBy(ModulithArchitectureTest::beanName, TreeMap::new,
                    Collectors.mapping(JavaClass::getName, Collectors.toList())));

        assertThat(classesByBeanName)
            .allSatisfy((beanName, classNames) -> assertThat(classNames).as("빈 이름 '%s'가 겹칩니다", beanName).hasSize(1));
    }

    /**
     * Spring의 AnnotationBeanNameGenerator와 같이 명시한 이름을 쓰고, 없으면 짧은 클래스 이름의 첫 글자를 소문자로 바꾼다.
     */
    private static String beanName(JavaClass javaClass) {
        return javaClass.getAnnotations()
            .stream()
            .filter(annotation -> annotation.getRawType().isEquivalentTo(Component.class)
                    || annotation.getRawType().isMetaAnnotatedWith(Component.class))
            .map(annotation -> annotation.get("value").orElse(""))
            .filter(value -> value instanceof String name && !name.isBlank())
            .map(String.class::cast)
            .findFirst()
            .orElseGet(() -> Introspector.decapitalize(
                    javaClass.getName().substring(javaClass.getPackageName().length() + 1).replace('$', '.')));
    }

}
