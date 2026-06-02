package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class JdbcPolicyVectorRepositoryDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String COMPLETION_RECORD =
            "version-updates/EXEC_PLAN_V5_A1_JDBC_POLICY_VECTOR_REPOSITORY.md";

    private static final List<String> V5_A1_DOCS = List.of(
            "README.md",
            "docs/agent/RAG_POLICY_RETRIEVAL_CONTRACT.md",
            "docs/decisions/DECISION_V4_RAG_VECTOR_STORE.md",
            "docs/demo/V4_PGVECTOR_LOCAL_SETUP.md",
            "docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "version-updates/V4_RELEASE_SUMMARY.md",
            COMPLETION_RECORD);

    @Test
    void completionRecordExistsAndDocumentsV5A1Boundary() throws IOException {
        String completion = projectText(COMPLETION_RECORD);
        String readme = projectText("README.md");

        assertThat(readme).contains(COMPLETION_RECORD);
        assertThat(completion).contains(
                "Status: Completed",
                "JdbcPolicyVectorRepository Boundary",
                "PGvector Profile / Property Boundary",
                "SQL / Row Mapping Boundary",
                "Sanitized Error Boundary",
                "Runtime Non-change Boundary",
                "Default Offline Boundary",
                "TASK_COMPLETE");
    }

    @Test
    void docsStateRepositoryIsOptInAndDefaultPathStaysOffline() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "V5.A.1",
                "JdbcPolicyVectorRepository",
                "explicit opt-in",
                "rag-postgres",
                "pgvector",
                "default profile does not create",
                "default offline",
                "does not require real LLMs",
                "API keys",
                "PostgreSQL",
                "PGvector",
                "Docker");
    }

    @Test
    void docsKeepRuntimeSearchAndToolBoundariesUnchanged() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "does not change `search_aftersale_policy` retrieval algorithms",
                "does not add a public RAG HTTP endpoint",
                "LOW-risk read-only ToolRegistry tool",
                "RAG evidence remains policy evidence only",
                "not a replacement for `ToolRegistry`");
        assertThat(docs).doesNotContain(
                "public RAG HTTP endpoint completed",
                "search_aftersale_policy runtime changed",
                "retrieval algorithm changed",
                "ToolRegistry replaced");
    }

    @Test
    void docsKeepLivePgVectorSpringAiVectorStoreAndMigrationAsFutureWork() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "live PGvector validation is not completed",
                "Spring AI VectorStore production path",
                "No database migration baseline",
                "No Spring AI `VectorStore` production path is enabled",
                "No public RAG HTTP endpoint is added");
        assertThat(docs.toLowerCase(Locale.ROOT)).doesNotContain(
                "live pgvector validation completed",
                "spring ai vectorstore production path completed",
                "flyway baseline completed",
                "liquibase baseline completed",
                "public rag endpoint completed");
    }

    @Test
    void docsAreSecretPathAndProductionOverclaimSafe() throws IOException {
        for (String path : V5_A1_DOCS) {
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
        for (String path : V5_A1_DOCS) {
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
