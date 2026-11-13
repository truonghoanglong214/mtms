package com.mtms.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ModuleBoundaryTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.mtms");
    }

    @Test
    @DisplayName("Domain layers should not depend on web or infrastructure layers")
    void domainShouldNotDependOnWebOrInfra() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..web..", "..infrastructure..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    @Test
    @DisplayName("Common package should not depend on any business domain modules")
    void commonShouldNotDependOnModules() {
        noClasses()
                .that().resideInAPackage("com.mtms.common..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.mtms.organization..",
                        "com.mtms.identity..",
                        "com.mtms.revenue..",
                        "com.mtms.workforce..",
                        "com.mtms.finance..",
                        "com.mtms.inventory..",
                        "com.mtms.closing..",
                        "com.mtms.capital.."
                )
                .allowEmptyShould(true)
                .check(importedClasses);
    }
}
