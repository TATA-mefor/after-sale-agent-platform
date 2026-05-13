package com.example.aftersale;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private static final JavaClasses APPLICATION_CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.example.aftersale");

    @Test
    void apiLayerMustNotAccessRepositories() {
        noClasses()
                .that()
                .resideInAPackage("..api..")
                .should()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("Repository")
                .because("API classes must call application services instead of repositories directly.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .resideInAPackage("..api..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure..repository..")
                .because("API classes must call application services instead of repositories directly.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void domainLayerMustNotDependOnSpringWeb() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("org.springframework.web..")
                .because("domain code must stay independent from HTTP and Spring Web concerns.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void agentModuleMustNotAccessBusinessRepositoriesDirectly() {
        noClasses()
                .that()
                .resideInAPackage("..agent..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..order..infrastructure..repository..",
                        "..ticket..infrastructure..repository..",
                        "..policy..infrastructure..repository..")
                .because("agent code must reach business capability through tools, not repositories.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void toolModuleMustNotAccessRepositoriesDirectly() {
        noClasses()
                .that()
                .resideInAPackage("..tool..")
                .should()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("Repository")
                .because("tools must call business application services instead of repositories directly.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void businessModulesMustNotDependOnAgentModule() {
        noClasses()
                .that()
                .resideInAnyPackage("..order..", "..ticket..", "..policy..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..agent..")
                .because("business modules must not depend on the agent orchestration module.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }
}
