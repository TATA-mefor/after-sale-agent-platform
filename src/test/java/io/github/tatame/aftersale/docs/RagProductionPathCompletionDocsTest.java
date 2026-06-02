package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class RagProductionPathCompletionDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String V5_A_COMPLETION =
            "docs/exec-plans/completed/EXEC_PLAN_V5_A_RAG_PRODUCTION_PATH_COMPLETION.md";

    private static final String V5_A_SUMMARY = "docs/release/V5_A_RAG_PRODUCTION_PATH_SUMMARY.md";

    private static final List<String> V5_A_COMPLETION_DOCS = List.of(
            "README.md",
            "docs/agent/RAG_POLICY_RETRIEVAL_CONTRACT.md",
            "docs/decisions/DECISION_V4_RAG_VECTOR_STORE.md",
            "docs/demo/V4_PGVECTOR_LOCAL_SETUP.md",
            "docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "docs/exec-plans/active/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "docs/release/V4_RELEASE_SUMMARY.md",
            V5_A_COMPLETION,
            V5_A_SUMMARY);

    @Test
    void completionRecordsAndSummaryExist() throws IOException {
        assertThat(projectText("docs/exec-plans/completed/EXEC_PLAN_V5_A1_JDBC_POLICY_VECTOR_REPOSITORY.md"))
                .contains("Status: Completed", "TASK_COMPLETE");
        assertThat(projectText("docs/exec-plans/completed/EXEC_PLAN_V5_A2_SCHEMA_INIT_VERSION_BASELINE.md"))
                .contains("Status: Completed", "TASK_COMPLETE");
        assertThat(projectText("docs/exec-plans/completed/EXEC_PLAN_V5_A3_PGVECTOR_CONNECTIVITY_SMOKE_TEST.md"))
                .contains("Status: Completed", "TASK_COMPLETE");
        assertThat(projectText(V5_A_COMPLETION))
                .contains("Status: Completed", "V5.A completed", "Completion Signal", "TASK_COMPLETE");
        assertThat(projectText(V5_A_SUMMARY)).contains("Status: Completed", "What V5.A Delivered");
    }

    @Test
    void docsMarkV5ACompletedAndV5BPlanned() throws IOException {
        String docs = combinedDocs();

        assertThat(projectText("README.md")).contains(
                "V5.A RAG Production Path Completion",
                "V5.A completed the RAG production path foundation",
                V5_A_COMPLETION,
                V5_A_SUMMARY);
        assertThat(projectText("docs/exec-plans/active/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md")).contains(
                "V5.A RAG production path foundation completed",
                "V5.A.4 docs / completion record 已完成",
                "V5.B planned");
        assertThat(docs).contains(
                "V5.A completed",
                "RAG production path foundation",
                "V5.A.1",
                "V5.A.2",
                "V5.A.3",
                "V5.A.4");
    }

    @Test
    void docsDescribeDefaultFakePathAndOptInPgVectorPath() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "InMemoryPolicyVectorRepository",
                "PolicyVectorRepository",
                "JdbcPolicyVectorRepository",
                "explicit opt-in",
                "default `mvn test` does not run live PGvector smoke",
                "Default validation does not connect PostgreSQL / PGvector",
                "fake / in-memory",
                "fake / fixed vectors");
    }

    @Test
    void docsDescribeSmokeBoundaryWithoutRagQualityOverclaim() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "JdbcPolicyVectorRepositorySmokeTest",
                "-Dlive.rag=true",
                "mvn test \"-Dtest=JdbcPolicyVectorRepositorySmokeTest\" \"-Dlive.rag=true\"",
                "AFTERSALE_PGVECTOR_URL",
                "AFTERSALE_PGVECTOR_USERNAME",
                "AFTERSALE_PGVECTOR_PASSWORD",
                "connectivity",
                "does not validate RAG quality",
                "real embedding quality validation is not completed");
        assertThat(docs.toLowerCase(Locale.ROOT)).doesNotContain(
                "rag quality completed by v5.a",
                "real embedding quality validation completed",
                "spring ai vectorstore production path completed");
    }

    @Test
    void docsKeepFutureProductionHardeningClear() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "Flyway / Liquibase migration management remains pending V5.B.2",
                "Spring AI `VectorStore` production path is not enabled",
                "production auth / RBAC is not completed",
                "production monitoring is not completed",
                "V5.B",
                "public RAG HTTP endpoint");
        assertThat(docs.toLowerCase(Locale.ROOT)).doesNotContain(
                "flyway baseline completed",
                "liquibase baseline completed",
                "production auth completed",
                "production monitoring completed",
                "production deployment completed");
    }

    @Test
    void docsKeepToolRegistryEvidenceAndApprovalBoundaries() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "search_aftersale_policy",
                "LOW-risk read-only ToolRegistry tool",
                "RAG evidence remains policy evidence only",
                "RAG score is not business decision confidence",
                "LLMs must not directly execute tools",
                "High-risk actions still require Approval");
    }

    @Test
    void docsAreSecretPathAndExternalIntegrationOverclaimSafe() throws IOException {
        for (String path : V5_A_COMPLETION_DOCS) {
            assertSafeText(path, projectText(path));
        }
    }

    private static void assertSafeText(String path, String text) {
        String lower = text.toLowerCase(Locale.ROOT);

        assertThat(lower).as(path).doesNotContain(
                "d:/",
                "d:\\",
                "c:/",
                "c:\\",
                "/users/",
                "/home/",
                "openai_api_key=",
                "dashscope_api_key=",
                "spring_ai_openai_api_key=",
                "password=prod",
                "password=production",
                "token=",
                "secret=",
                "sk-",
                "real refund integration completed",
                "real exchange integration completed",
                "real compensation integration completed",
                "real payment integration completed",
                "real logistics integration completed",
                "production deployment completed",
                "production monitoring completed",
                "production auth completed");
    }

    private static String combinedDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : V5_A_COMPLETION_DOCS) {
            builder.append(projectText(path)).append('\n');
        }
        return builder.toString();
    }

    private static String projectText(String path) throws IOException {
        Path file = PROJECT_ROOT.resolve(path);
        assertThat(Files.exists(file)).as(path + " should exist").isTrue();
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
