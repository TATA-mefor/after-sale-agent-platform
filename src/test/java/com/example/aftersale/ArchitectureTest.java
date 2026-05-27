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

        noClasses()
                .that()
                .resideInAPackage("..agent.application.workspace..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "javax.sql..",
                        "org.springframework.jdbc..",
                        "org.springframework.ai..",
                        "org.springframework.ai.vectorstore..",
                        "..policy.rag.infrastructure..")
                .because("Workspace evidence mapping must stay a local summary and avoid RAG infrastructure.")
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
    void agentHandlerAndSkillMustNotDependOnPgVectorOrJdbcClients() {
        noClasses()
                .that()
                .resideInAnyPackage("..agent.application..", "..agent.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "javax.sql..",
                        "org.springframework.jdbc..",
                        "org.springframework.ai.vectorstore..",
                        "..policy.rag.infrastructure.pgvector..",
                        "..policy.rag.infrastructure.memory..")
                .because("Agent application, handler, and skill code must not access vector infrastructure directly.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void ragVectorDomainMustStayPureDomainContract() {
        noClasses()
                .that()
                .resideInAPackage("..policy.rag.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "javax.sql..",
                        "org.springframework.jdbc..",
                        "org.springframework.ai..",
                        "..policy.rag.infrastructure..")
                .because("RAG vector domain contracts must not depend on Spring, JDBC, Spring AI, or infrastructure.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void ragSearchContractMustStayPureAndRepositoryFree() {
        noClasses()
                .that()
                .resideInAPackage("..policy.rag.search..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "javax.sql..",
                        "org.springframework.jdbc..",
                        "org.springframework.ai..",
                        "org.springframework.ai.vectorstore..",
                        "..policy.rag.infrastructure..")
                .because("RAG search contracts and mappers must stay free of Spring, JDBC, Spring AI, and infra.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .resideInAPackage("..policy.rag.search..")
                .should()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("Repository")
                .because("V4.5.1 mappers convert given results and must not access repositories.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void ragEvidenceMergeServiceMustStayPureAndRepositoryFree() {
        noClasses()
                .that()
                .haveSimpleName("RagPolicyEvidenceMergeService")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework.web..",
                        "javax.sql..",
                        "org.springframework.jdbc..",
                        "org.springframework.ai..",
                        "org.springframework.ai.vectorstore..",
                        "..policy.rag.infrastructure..",
                        "..policy.infrastructure.repository..")
                .because("V4.5.2 merge service is pure evidence merge logic, not runtime retrieval wiring.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .haveSimpleName("RagPolicyEvidenceMergeService")
                .should()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("Repository")
                .because("merge service must not access keyword or vector repositories.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .haveSimpleName("RagPolicyEvidenceMergeService")
                .should()
                .dependOnClassesThat()
                .haveSimpleName("EmbeddingClient")
                .because("merge service must not call embedding providers.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void ragPolicySearchApplicationServiceMustUseOnlyProjectContracts() {
        noClasses()
                .that()
                .haveSimpleName("RagPolicySearchApplicationService")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework.web..",
                        "javax.sql..",
                        "org.springframework.jdbc..",
                        "org.springframework.ai..",
                        "org.springframework.ai.vectorstore..",
                        "..policy.rag.infrastructure.pgvector..",
                        "..policy.rag.infrastructure.springai..",
                        "..policy.rag.infrastructure.memory..")
                .because("V4.5.3 runtime search may use EmbeddingClient and PolicyVectorRepository contracts only.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void searchPolicyToolExecutorMustNotDependOnVectorInfrastructureOrProviderImplementations() {
        noClasses()
                .that()
                .haveSimpleName("SearchAfterSalePolicyToolExecutor")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "javax.sql..",
                        "org.springframework.jdbc..",
                        "org.springframework.ai..",
                        "org.springframework.ai.vectorstore..",
                        "..policy.rag.infrastructure.pgvector..",
                        "..policy.rag.infrastructure.springai..",
                        "..policy.rag.infrastructure.memory..")
                .because("the search tool executor must call the RAG application service, not provider infrastructure.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .haveSimpleName("SearchAfterSalePolicyToolExecutor")
                .should()
                .dependOnClassesThat()
                .haveSimpleName("PolicyVectorRepository")
                .because("the tool executor must not directly access vector repositories.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void executionTreeMustRemainReadOnlyAndNotDependOnRagRuntimeInfrastructure() {
        noClasses()
                .that()
                .haveSimpleName("ExecutionTreeApplicationService")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "javax.sql..",
                        "org.springframework.jdbc..",
                        "org.springframework.ai..",
                        "org.springframework.ai.vectorstore..",
                        "..policy.rag.infrastructure..")
                .because("Execution Tree is a read-only explanation view and must not call RAG infrastructure.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .haveSimpleName("ExecutionTreeApplicationService")
                .should()
                .dependOnClassesThat()
                .haveSimpleName("EmbeddingClient")
                .because("Execution Tree must display recorded evidence, not perform embedding.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .haveSimpleName("ExecutionTreeApplicationService")
                .should()
                .dependOnClassesThat()
                .haveSimpleName("PolicyVectorRepository")
                .because("Execution Tree must display trace/workspace evidence, not query vector repositories.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void agentHandlerAndSkillMustNotDependOnPolicyVectorRepositoryContract() {
        noClasses()
                .that()
                .resideInAnyPackage("..agent.application..", "..agent.domain..")
                .should()
                .dependOnClassesThat()
                .haveSimpleName("PolicyVectorRepository")
                .because("Agent code must reach RAG evidence through tools, not vector repository contracts directly.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .resideInAnyPackage("..agent.application..", "..agent.domain..")
                .should()
                .dependOnClassesThat()
                .haveSimpleName("InMemoryPolicyVectorRepository")
                .because("Agent code must not depend on fake vector repository implementations directly.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .resideInAnyPackage("..agent.application..", "..agent.domain..")
                .should()
                .dependOnClassesThat()
                .haveSimpleName("EmbeddingClient")
                .because("Agent code must reach vector retrieval through ToolRegistry, not embedding clients directly.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void agentHandlerAndSkillMustNotDependOnRagSearchPreparationModels() {
        noClasses()
                .that()
                .resideInAnyPackage("..agent.application..", "..agent.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..policy.rag.search..")
                .because("Agent, handler, and skill code must not bypass the search tool into RAG search internals.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void agentHandlerAndSkillMustNotDependOnPolicyIngestionRepositoryContract() {
        noClasses()
                .that()
                .resideInAnyPackage("..agent.application..", "..agent.domain..")
                .should()
                .dependOnClassesThat()
                .haveSimpleName("PolicyIngestionRepository")
                .because("Policy ingestion is an admin pipeline capability, not an Agent runtime tool.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .resideInAnyPackage("..agent.application..", "..agent.domain..")
                .should()
                .dependOnClassesThat()
                .haveSimpleName("InMemoryPolicyIngestionRepository")
                .because("Agent code must not depend on ingestion infrastructure directly.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .resideInAnyPackage("..agent.application..", "..agent.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..policy.rag.ingestion.application..")
                .because("chunking and dedup services are admin pipeline helpers, not Agent runtime tools.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void policyVectorRepositoryContractMustNotDependOnInfrastructure() {
        noClasses()
                .that()
                .haveSimpleName("PolicyVectorRepository")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .because("repository contracts must stay independent from infrastructure implementations.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void policyIngestionDomainMustStayPureDomainContract() {
        noClasses()
                .that()
                .resideInAPackage("..policy.rag.ingestion.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "javax.sql..",
                        "org.springframework.jdbc..",
                        "org.springframework.ai..",
                        "..policy.rag.infrastructure..",
                        "..policy.rag.ingestion.infrastructure..")
                .because("policy ingestion domain contracts must stay free of Spring, JDBC, Spring AI, and infra.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void policyIngestionRepositoryContractMustNotDependOnInfrastructure() {
        noClasses()
                .that()
                .haveSimpleName("PolicyIngestionRepository")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .because("ingestion repository contracts must stay independent from infrastructure implementations.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void policyIngestionApplicationMustStayOfflineAndNotBypassAgentRuntime() {
        noClasses()
                .that()
                .resideInAnyPackage("..policy.rag.ingestion.application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework.web..",
                        "javax.sql..",
                        "org.springframework.jdbc..",
                        "org.springframework.ai..",
                        "org.springframework.ai.vectorstore..",
                        "..policy.rag.infrastructure.pgvector..",
                        "..policy.rag.infrastructure.springai..",
                        "..policy.rag.infrastructure.memory..",
                        "..order..infrastructure..repository..",
                        "..ticket..infrastructure..repository..",
                        "..policy..infrastructure..repository..")
                .because("ingestion application services must stay offline and avoid provider implementations.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .resideInAnyPackage("..policy.rag.ingestion.application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..tool..", "..agent.application.handler..", "..agent.application.skill..")
                .because("ingestion helpers must not become Agent tools or bypass ToolRegistry.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .resideInAnyPackage("..policy.rag.ingestion.application..")
                .should()
                .dependOnClassesThat()
                .haveSimpleName("SpringAiEmbeddingClient")
                .because("V4.4.3 may use EmbeddingClient but must not call the real Spring AI adapter.")
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

    @Test
    void pgVectorInfrastructureMustStayInRagBoundaryAndNotAccessRepositories() {
        noClasses()
                .that()
                .resideInAnyPackage("..policy.rag.infrastructure.pgvector..")
                .should()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("Repository")
                .because("PGvector profile boundary must not access business repositories.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .resideInAnyPackage("..policy.rag.infrastructure.pgvector..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..tool..", "..agent.application.handler..", "..agent.application.skill..")
                .because("PGvector infrastructure must not bypass ToolRegistry or Agent execution boundaries.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void ingestionMemoryInfrastructureMustStayOfflineAndNotAccessBusinessRepositories() {
        noClasses()
                .that()
                .resideInAnyPackage("..policy.rag.ingestion.infrastructure.memory..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "javax.sql..",
                        "org.springframework.jdbc..",
                        "org.springframework.ai..",
                        "..order..infrastructure..repository..",
                        "..ticket..infrastructure..repository..",
                        "..policy..infrastructure..repository..")
                .because("ingestion memory infrastructure must stay offline and avoid business repositories.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .resideInAnyPackage("..policy.rag.ingestion.infrastructure.memory..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..tool..", "..agent.application.handler..", "..agent.application.skill..")
                .because("ingestion memory infrastructure must not bypass ToolRegistry or Agent execution boundaries.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }

    @Test
    void fakeVectorInfrastructureMustStayOfflineAndNotAccessBusinessRepositories() {
        noClasses()
                .that()
                .resideInAnyPackage("..policy.rag.infrastructure.memory..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "javax.sql..",
                        "org.springframework.jdbc..",
                        "org.springframework.ai..",
                        "..order..infrastructure..repository..",
                        "..ticket..infrastructure..repository..",
                        "..policy..infrastructure..repository..")
                .because("fake vector infrastructure must stay offline and must not access business repositories.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);

        noClasses()
                .that()
                .resideInAnyPackage("..policy.rag.infrastructure.memory..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..tool..", "..agent.application.handler..", "..agent.application.skill..")
                .because("fake vector infrastructure must not bypass ToolRegistry or Agent execution boundaries.")
                .allowEmptyShould(true)
                .check(APPLICATION_CLASSES);
    }
}
