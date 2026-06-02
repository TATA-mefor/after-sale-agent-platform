package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class SchemaVersionBaselineDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String SCHEMA = "src/main/resources/schema-rag-postgres.sql";

    private static final String COMPLETION_RECORD =
            "version-updates/EXEC_PLAN_V5_A2_SCHEMA_INIT_VERSION_BASELINE.md";

    private static final List<String> V5_A2_DOCS = List.of(
            SCHEMA,
            "README.md",
            "version-updates/V4_FACTS.md",
            "docs/agent/RAG_POLICY_RETRIEVAL_CONTRACT.md",
            "docs/decisions/DECISION_V4_RAG_VECTOR_STORE.md",
            "docs/demo/V4_PGVECTOR_LOCAL_SETUP.md",
            "docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            COMPLETION_RECORD);

    @Test
    void schemaFileContainsVersionBaselineComments() throws IOException {
        String schema = projectText(SCHEMA);

        assertThat(schema).contains(
                "V5.A.2 schema version: 2026-06-01-001",
                "Intended for: JdbcPolicyVectorRepository",
                "Migration framework: pending V5.B.2",
                "Current initialization path",
                "Default test boundary");
        assertThat(schema).doesNotContain("CREATE TABLE IF NOT EXISTS v5_a2");
    }

    @Test
    void docsAndCompletionRecordDescribeV5A2Scope() throws IOException {
        String docs = combinedDocs();

        assertThat(projectText(COMPLETION_RECORD)).contains("Status: Completed", "TASK_COMPLETE");
        assertThat(projectText("README.md") + "\n" + projectText("version-updates/V4_FACTS.md")).contains(
                "V5.A.2",
                "schema version baseline",
                "2026-06-01-001",
                COMPLETION_RECORD);
        assertThat(projectText("docs/demo/V4_PGVECTOR_LOCAL_SETUP.md")).contains(
                "Schema Version Baseline",
                "2026-06-01-001",
                "Existing PostgreSQL volumes do not rerun");
        assertThat(docs).contains(
                "V5.A.2",
                "schema version baseline",
                "JdbcPolicyVectorRepository",
                "PGvector policy evidence search");
    }

    @Test
    void docsKeepMigrationLivePgVectorAndVectorStoreAsFutureOrOptIn() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "Flyway / Liquibase migration management remains pending V5.B.2",
                "Live PGvector smoke validation is later completed by",
                "V5.A.3 later completes the opt-in live PGvector",
                "default fake / in-memory",
                "Spring AI `VectorStore` production path is not enabled",
                "production deployment is not completed");
    }

    @Test
    void activePlanMarksV5A2CompletedAndNextStepsPlanned() throws IOException {
        String activePlan = projectText("version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md");

        assertThat(activePlan).contains(
                "V5.A.2 schema init / version baseline 已完成",
                "V5.A.3 PGvector connectivity smoke 已完成",
                "V5.A.4",
                "V5.B");
    }

    @Test
    void docsAreSecretPathAndOverclaimSafe() throws IOException {
        for (String path : V5_A2_DOCS) {
            assertSafeText(path, projectText(path));
        }
        assertNoCompletionOverclaims(combinedDocs());
    }

    private static void assertNoCompletionOverclaims(String text) {
        String lower = text.toLowerCase(Locale.ROOT);

        assertThat(lower).doesNotContain(
                "flyway baseline completed",
                "liquibase baseline completed",
                "live pgvector validation completed",
                "spring ai vectorstore production path completed",
                "production deployment completed",
                "public rag endpoint completed",
                "real refund integration completed",
                "real exchange integration completed",
                "real compensation integration completed",
                "real payment integration completed",
                "real logistics integration completed");
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
                "sk-");
    }

    private static String combinedDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : V5_A2_DOCS) {
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
