package com.example.aftersale;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * 验证模块依赖边界，防止 API、Agent、Tool、Workspace 和 LLM adapter 绕过应用服务或工具注册表。
 */
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

    @Test
    void agentApplicationServiceMustNotDependOnLlmInfrastructure() {
        noClasses()
                .that()
                .haveSimpleName("AgentApplicationService")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..agent.infrastructure.llm..")
                .because("AgentApplicationService must depend on AgentPlanner, not LLM infrastructure.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void apiLayerMustNotDependOnLlmPlanner() {
        noClasses()
                .that()
                .resideInAPackage("..api..")
                .should()
                .dependOnClassesThat()
                .haveSimpleName("LlmAgentPlanner")
                .because("Controllers must not select or call concrete LLM planner implementations.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void llmInfrastructureMustNotAccessRepositories() {
        noClasses()
                .that()
                .resideInAPackage("..agent.infrastructure.llm..")
                .should()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("Repository")
                .because("LLM adapters must only plan and must not access persistence directly.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void specialistHandlersMustNotAccessRepositoriesOrLlmInfrastructure() {
        noClasses()
                .that()
                .resideInAPackage("..agent.application.handler..")
                .should()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("Repository")
                .because("specialist handlers must use ToolRegistry instead of repositories.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .resideInAPackage("..agent.application.handler..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..agent.infrastructure.llm..", "org.springframework.web..")
                .because("specialist handlers must not call LLM infrastructure or depend on HTTP concerns.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void agentWorkspaceMustNotAccessRepositoriesOrToolExecutors() {
        noClasses()
                .that()
                .resideInAPackage("..agent.application.workspace..")
                .should()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("Repository")
                .because("AgentWorkspace is in-run structured memory and must not access repositories.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .resideInAPackage("..agent.application.workspace..")
                .should()
                .dependOnClassesThat()
                .haveSimpleName("ToolRegistry")
                .because("AgentWorkspace must not execute tools or replace ToolRegistry.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void approvalApiAndAgentHandlersMustNotAccessApprovalRepositoryDirectly() {
        noClasses()
                .that()
                .resideInAPackage("..approval.api..")
                .should()
                .dependOnClassesThat()
                .haveSimpleName("ApprovalRepository")
                .because("Approval controllers must call ApprovalApplicationService instead of repositories.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .resideInAnyPackage("..agent..", "..agent.application.handler..")
                .should()
                .dependOnClassesThat()
                .haveSimpleName("ApprovalRepository")
                .because("Agent code and handlers must create approvals through application services.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void skillLayerMustKeepToolAndInfrastructureBoundaries() {
        noClasses()
                .that()
                .resideInAPackage("..agent.application.skill..")
                .should()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("Repository")
                .because("skills must coordinate through handlers and ToolRegistry, not repositories.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .resideInAPackage("..agent.application.skill..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework.web..", "..agent.infrastructure.llm..")
                .because("skills must not depend on HTTP or LLM infrastructure.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .resideInAPackage("..agent.application.skill..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework.ai..", "..vector..", "..rag..")
                .because("V4.1 skills must not access Spring AI, vector stores, or RAG infrastructure directly.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .resideInAPackage("..agent.application.skill..")
                .should()
                .dependOnClassesThat()
                .haveSimpleName("ToolExecutor")
                .because("skills must not call concrete tool executors directly.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void agentHandlerAndSkillMustNotDependOnSpringAiClasses() {
        noClasses()
                .that()
                .resideInAnyPackage("..agent.application..", "..agent.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("org.springframework.ai..")
                .because("Agent application, handler, and skill code must use project boundaries, not Spring AI APIs.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void springAiAdaptersMustStayInfrastructureAndNotAccessRepositories() {
        noClasses()
                .that()
                .resideInAnyPackage("..infrastructure.springai..")
                .should()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("Repository")
                .because("Spring AI adapters are provider adapters and must not access persistence.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }
}
