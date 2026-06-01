package com.example.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class PgVectorConnectivitySmokeDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String SMOKE_TEST =
            "src/test/java/com/example/aftersale/policy/rag/infrastructure/pgvector/"
                    + "JdbcPolicyVectorRepositorySmokeTest.java";

    private static final String COMPLETION_RECORD =
            "docs/exec-plans/completed/EXEC_PLAN_V5_A3_PGVECTOR_CONNECTIVITY_SMOKE_TEST.md";

    private static final List<String> V5_A3_DOCS = List.of(
            "README.md",
            "docs/agent/RAG_POLICY_RETRIEVAL_CONTRACT.md",
            "docs/decisions/DECISION_V4_RAG_VECTOR_STORE.md",
            "docs/demo/V4_PGVECTOR_LOCAL_SETUP.md",
            "docs/exec-plans/active/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "docs/release/V4_RELEASE_SUMMARY.md",
            COMPLETION_RECORD);

    @Test
    void smokeTestRequiresExplicitLiveFlagAndExistingPgVectorEnvironmentVariables() throws IOException {
        String source = projectText(SMOKE_TEST);

        assertThat(source).contains(
                "@Tag(\"live\")",
                "@EnabledIfSystemProperty(named = \"live.rag\", matches = \"true\")",
                "AFTERSALE_PGVECTOR_URL",
                "AFTERSALE_PGVECTOR_USERNAME",
                "AFTERSALE_PGVECTOR_PASSWORD",
                "AFTERSALE_PGVECTOR_SCHEMA",
                "assumeTrue",
                "fixed-vector");
        assertThat(source).doesNotContain(
                "AFTERSALE_RAG_PGVECTOR_JDBC_URL",
                "AFTERSALE_RAG_PGVECTOR_USERNAME",
                "AFTERSALE_RAG_PGVECTOR_PASSWORD");
    }

    @Test
    void docsAndCompletionRecordDescribeV5A3Boundary() throws IOException {
        String docs = combinedDocs();

        assertThat(projectText(COMPLETION_RECORD)).contains(
                "Status: Completed",
                "Smoke Test Boundary",
                "PGvector Connectivity Boundary",
                "Fake / Fixed Vector Boundary",
                "Schema Initialization Boundary",
                "Default Offline Boundary",
                "TASK_COMPLETE");
        assertThat(projectText("README.md")).contains(
                "V5.A.3",
                "PGvector connectivity smoke",
                COMPLETION_RECORD);
        assertThat(docs).contains(
                "mvn test -Dtest=JdbcPolicyVectorRepositorySmokeTest -Dlive.rag=true",
                "AFTERSALE_PGVECTOR_URL",
                "AFTERSALE_PGVECTOR_USERNAME",
                "AFTERSALE_PGVECTOR_PASSWORD",
                "AFTERSALE_PGVECTOR_SCHEMA",
                "default `mvn test` does not run live PGvector smoke",
                "fake / fixed vectors",
                "does not validate RAG quality",
                "CREATE EXTENSION");
    }

    @Test
    void docsKeepFutureWorkAndRuntimeBoundariesClear() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "V5.A.3 completed",
                "V5.A.4",
                "Flyway / Liquibase migration management remains pending V5.B.2",
                "Spring AI `VectorStore` production path is not enabled",
                "RAG evidence remains policy evidence only",
                "LOW-risk read-only ToolRegistry tool",
                "does not change `search_aftersale_policy` retrieval algorithms");
        assertThat(docs.toLowerCase(Locale.ROOT)).doesNotContain(
                "rag quality completed by v5.a.3",
                "spring ai vectorstore production path completed",
                "flyway baseline completed",
                "liquibase baseline completed",
                "public rag endpoint completed");
    }

    @Test
    void liveSkipClosureMentionsPgVectorSmoke() throws IOException {
        String source = projectText("src/test/java/com/example/aftersale/LiveTestSkipClosureTest.java");

        assertThat(source).contains(
                "JdbcPolicyVectorRepositorySmokeTest",
                "@EnabledIfSystemProperty(named = \\\"live.rag\\\", matches = \\\"true\\\")",
                "AFTERSALE_PGVECTOR_URL",
                "AFTERSALE_PGVECTOR_USERNAME",
                "AFTERSALE_PGVECTOR_PASSWORD");
    }

    @Test
    void docsAreSecretPathAndOverclaimSafe() throws IOException {
        for (String path : V5_A3_DOCS) {
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
        for (String path : V5_A3_DOCS) {
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
