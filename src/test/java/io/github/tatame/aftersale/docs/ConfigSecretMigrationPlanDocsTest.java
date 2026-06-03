package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ConfigSecretMigrationPlanDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String DECISION_DOC =
            "docs/decisions/DECISION_V5_B2_CONFIG_SECRET_MIGRATION.md";

    private static final String PLAN_DOC = "docs/deploy/CONFIG_SECRET_MIGRATION_PLAN.md";

    private static final String COMPLETED_PLAN =
            "docs/exec-plans/completed/EXEC_PLAN_V5_B2_1_CONFIG_SECRET_BOUNDARY.md";

    private static final String MIGRATION_DOC = "docs/deploy/MIGRATION_FOUNDATION.md";

    private static final String MIGRATION_COMPLETED_PLAN =
            "docs/exec-plans/completed/EXEC_PLAN_V5_B2_2_FLYWAY_MIGRATION_FOUNDATION.md";

    private static final List<String> V5_B2_1_DOCS = List.of(
            "README.md",
            "docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            "version-updates/V4_RELEASE_SUMMARY.md",
            "version-updates/V5_A_RAG_PRODUCTION_PATH_SUMMARY.md",
            DECISION_DOC,
            PLAN_DOC,
            COMPLETED_PLAN,
            MIGRATION_DOC,
            MIGRATION_COMPLETED_PLAN);

    @Test
    void configSecretMigrationDocsAndCompletionRecordExist() throws IOException {
        assertThat(projectText(DECISION_DOC)).contains(
                "Status: Completed",
                "Current Configuration Baseline",
                "Profile Matrix",
                "Secret Boundary",
                "Migration Framework Boundary",
                "TASK_COMPLETE");
        assertThat(projectText(PLAN_DOC)).contains(
                "配置、密钥与迁移治理方案",
                "Profile Matrix",
                "Default Offline Boundary",
                "TASK_COMPLETE");
        assertThat(projectText(COMPLETED_PLAN)).contains(
                "Status: Completed",
                "Configuration Baseline Boundary",
                "Completion Signal",
                "TASK_COMPLETE");
    }

    @Test
    void configurationBaselineAndProfileMatrixAreDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "application.yml",
                "default offline / local baseline",
                "application-prod.example.yml",
                "template only",
                "application-mysql.yml",
                "application-rag-postgres.yml",
                ".env.rag.example",
                "default / mysql / rag-postgres / prod-template",
                "explicit opt-in");
    }

    @Test
    void secretDockerCiAndLoggingBoundariesAreDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "AFTERSALE_PGVECTOR_URL",
                "AFTERSALE_PGVECTOR_USERNAME",
                "AFTERSALE_PGVECTOR_PASSWORD",
                "AFTERSALE_PGVECTOR_SCHEMA",
                "AFTERSALE_MYSQL_URL",
                "AFTERSALE_MYSQL_USERNAME",
                "AFTERSALE_MYSQL_PASSWORD",
                "Docker image does not contain secrets",
                "CI default gate",
                "does not inject live secrets",
                "logs",
                "health",
                "OpenAPI",
                "secret manager 是 future path",
                "secret manager 未实现");
    }

    @Test
    void flywayMigrationFoundationIsDocumentedWithRemainingRuntimeFollowUp() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "schema-rag-postgres.sql",
                "2026-06-01-001",
                "baseline reference",
                "Flyway migration foundation",
                "Liquibase 未引入",
                "spring.flyway.enabled: false",
                "schema-mysql.sql",
                "data-mysql.sql",
                "V5.B.2.3",
                "profile matrix runtime validation");
    }

    @Test
    void statusDocsLinkV5B21AndKeepLaterWorkPlanned() throws IOException {
        String readme = projectText("README.md");
        String roadmap = projectText("docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md");
        String validation = projectText("docs/quality/VALIDATION_COMMANDS.md");
        String quality = projectText("docs/quality/QUALITY_SCORE.md");
        String correctionPlan = projectText("version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md");

        assertThat(readme).contains(PLAN_DOC, DECISION_DOC, COMPLETED_PLAN, MIGRATION_DOC, MIGRATION_COMPLETED_PLAN);
        assertThat(roadmap).contains(
                "V5.B.2.1 Config + Secret Boundary 已完成文档基线",
                "V5.B.2.2",
                "Flyway migration foundation 已完成",
                "V5.B.2.3");
        assertThat(validation).contains(
                "V5.B.2.1 Config / Secret / Migration Plan Validation",
                "mvn test -Dtest=ConfigSecretMigrationPlanDocsTest",
                "V5.B.2.2 Flyway Migration Foundation Validation",
                "mvn test -Dtest=FlywayMigrationFoundationDocsTest");
        assertThat(quality).contains(
                "V5.B.2.1 Config / Secret Boundary",
                "V5.B.2.2 Flyway Migration Foundation",
                "Runtime non-change quality");
        assertThat(correctionPlan).contains(
                "V5.B.2.1 Config + Secret Boundary 已完成",
                "V5.B.2.2 Flyway migration foundation 已完成",
                "V5.B.2.3 planned");
    }

    @Test
    void defaultOfflineRuntimeNonChangeAndNonGoalsAreDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "does not modify `src/main/java`",
                "不修改 runtime",
                "real LLM",
                "API Key",
                "PostgreSQL",
                "PGvector",
                "Docker",
                "MySQL",
                "Redis",
                "external network",
                "Spring AI live calls",
                "Docker Compose",
                "ToolRegistry",
                "search_aftersale_policy",
                "RAG runtime");
    }

    @Test
    void docsDoNotContainSecretsLocalPathsOrProductionOverclaims() throws IOException {
        for (String path : V5_B2_1_DOCS) {
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
        for (String path : V5_B2_1_DOCS) {
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
