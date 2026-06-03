package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ProfileMatrixValidationDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String COMPLETED_PLAN =
            "docs/exec-plans/completed/EXEC_PLAN_V5_B2_3_PROFILE_MATRIX_VALIDATION.md";

    private static final List<String> V5_B2_3_DOCS = List.of(
            "README.md",
            "docs/deploy/CONFIG_SECRET_MIGRATION_PLAN.md",
            "docs/deploy/MIGRATION_FOUNDATION.md",
            "docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "docs/decisions/DECISION_V5_B2_CONFIG_SECRET_MIGRATION.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            "version-updates/V5_A_RAG_PRODUCTION_PATH_SUMMARY.md",
            COMPLETED_PLAN);

    @Test
    void completionRecordExistsAndMarksProfileMatrixValidationCompleted() throws IOException {
        String completedPlan = projectText(COMPLETED_PLAN);

        assertThat(completedPlan).contains(
                "Status: Completed",
                "Default Profile Boundary",
                "MySQL Profile Boundary",
                "RAG PGvector Profile Boundary",
                "Production Template Boundary",
                "Flyway Boundary",
                "Live Test Boundary",
                "Runtime Non-change Boundary",
                "TASK_COMPLETE");
    }

    @Test
    void statusDocsRecordV5B23AndCurrentV5B2ScopeCompleted() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "V5.B.2.3 Profile Matrix Validation",
                "profile matrix validation harness completed",
                "V5.B.2 current scope completed",
                COMPLETED_PLAN,
                "mvn test -Dtest=ProfileMatrixValidationTest",
                "mvn test -Dtest=ProfileMatrixValidationDocsTest");
    }

    @Test
    void profileBoundariesAreDocumentedWithoutRuntimeChangeClaims() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "default offline / local baseline",
                "application-mysql.yml",
                "application-rag-postgres.yml",
                "application-prod.example.yml",
                "template only",
                "explicit opt-in",
                "AFTERSALE_MYSQL_URL",
                "AFTERSALE_MYSQL_USERNAME",
                "AFTERSALE_MYSQL_PASSWORD",
                "AFTERSALE_PGVECTOR_URL",
                "AFTERSALE_PGVECTOR_USERNAME",
                "AFTERSALE_PGVECTOR_PASSWORD",
                "AFTERSALE_PGVECTOR_SCHEMA",
                "AFTERSALE_RAG_FLYWAY_ENABLED:false",
                "AFTERSALE_FLYWAY_ENABLED:false",
                "runtime profile behavior was not changed");
        assertThat(docs).doesNotContain(
                "AFTERSALE_RAG_PGVECTOR_URL",
                "AFTERSALE_RAG_PGVECTOR_USERNAME",
                "AFTERSALE_RAG_PGVECTOR_PASSWORD");
    }

    @Test
    void liveFlywayAndDefaultOfflineBoundariesAreDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "Flyway remains disabled by default",
                "classpath:db/migration/mysql",
                "classpath:db/migration/pgvector",
                "live PGvector smoke",
                "-Dlive.rag=true",
                "CREATE EXTENSION IF NOT EXISTS vector",
                "skip",
                "real LLM",
                "API Key",
                "PostgreSQL",
                "PGvector",
                "Docker",
                "MySQL",
                "Redis",
                "external network");
    }

    @Test
    void docsKeepSecretManagerAndProductionCapabilitiesAsFutureWork() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);

        assertThat(docs).contains(
                "secret manager 未实现",
                "production auth / RBAC 未完成",
                "production monitoring 未完成");
        assertThat(lower).contains(
                "secret manager is not implemented",
                "production deployment is not completed",
                "real refund / exchange / payment / logistics integrations are not connected");
    }

    @Test
    void docsDoNotContainSecretsLocalPathsOrOverclaims() throws IOException {
        for (String path : V5_B2_3_DOCS) {
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
                "secret manager completed",
                "liquibase completed",
                "flyway enabled by default",
                "profile matrix runtime validation completed",
                "production deployment completed",
                "production auth completed",
                "production monitoring completed",
                "真实退款已接入",
                "真实换货已接入",
                "真实补偿已接入",
                "真实支付已接入",
                "真实物流已接入");
    }

    private static String combinedDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : V5_B2_3_DOCS) {
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
