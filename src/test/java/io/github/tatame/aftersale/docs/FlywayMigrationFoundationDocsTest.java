package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class FlywayMigrationFoundationDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String MIGRATION_DOC = "docs/deploy/MIGRATION_FOUNDATION.md";

    private static final String COMPLETED_PLAN =
            "docs/exec-plans/completed/EXEC_PLAN_V5_B2_2_FLYWAY_MIGRATION_FOUNDATION.md";

    private static final String MYSQL_MIGRATION =
            "src/main/resources/db/migration/mysql/V20260603001__mysql_baseline.sql";

    private static final String PGVECTOR_MIGRATION =
            "src/main/resources/db/migration/pgvector/V20260601001__pgvector_policy_rag_baseline.sql";

    private static final List<String> V5_B2_2_DOCS = List.of(
            "README.md",
            "docs/deploy/CONFIG_SECRET_MIGRATION_PLAN.md",
            "docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "docs/decisions/DECISION_V5_B2_CONFIG_SECRET_MIGRATION.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            MIGRATION_DOC,
            COMPLETED_PLAN);

    @Test
    void flywayDependenciesAreAddedWithoutLiquibaseOrVersionScattering() throws IOException {
        String pom = projectText("pom.xml");

        assertThat(pom).contains(
                "<groupId>io.github.tatame</groupId>",
                "<artifactId>after-sale-agent-platform</artifactId>",
                "<artifactId>flyway-core</artifactId>",
                "<artifactId>flyway-database-postgresql</artifactId>",
                "<artifactId>flyway-mysql</artifactId>");
        assertThat(pom).doesNotContain(
                "<artifactId>liquibase-core</artifactId>",
                "<flyway.version>",
                "<liquibase.version>");
    }

    @Test
    void flywayIsDisabledByDefaultAndProfileSpecificOnly() throws IOException {
        String application = projectText("src/main/resources/application.yml");
        String mysql = projectText("src/main/resources/application-mysql.yml");
        String ragPostgres = projectText("src/main/resources/application-rag-postgres.yml");

        assertThat(application).contains("flyway:", "enabled: false");
        assertThat(mysql).contains(
                "enabled: ${AFTERSALE_FLYWAY_ENABLED:false}",
                "locations: classpath:db/migration/mysql");
        assertThat(ragPostgres).contains(
                "enabled: ${AFTERSALE_RAG_FLYWAY_ENABLED:false}",
                "locations: classpath:db/migration/pgvector");
        assertThat(ragPostgres).contains(
                "AFTERSALE_PGVECTOR_URL",
                "AFTERSALE_PGVECTOR_USERNAME",
                "AFTERSALE_PGVECTOR_PASSWORD",
                "AFTERSALE_PGVECTOR_SCHEMA");
    }

    @Test
    void pgvectorBaselineMigrationCopiesSchemaOnlyBoundary() throws IOException {
        String migration = projectText(PGVECTOR_MIGRATION);

        assertThat(migration).contains(
                "Source baseline: src/main/resources/schema-rag-postgres.sql",
                "Source schema version: 2026-06-01-001",
                "JdbcPolicyVectorRepository / PGvector policy evidence search",
                "CREATE EXTENSION IF NOT EXISTS vector",
                "CREATE TABLE IF NOT EXISTS policy_documents",
                "CREATE TABLE IF NOT EXISTS policy_chunks",
                "CREATE TABLE IF NOT EXISTS policy_embeddings",
                "CREATE INDEX IF NOT EXISTS idx_policy_documents_category_product",
                "CREATE INDEX IF NOT EXISTS idx_policy_embeddings_chunk");
        assertThat(migration.toLowerCase(Locale.ROOT)).doesNotContain(
                "insert into",
                "api_key",
                "token=",
                "raw prompt",
                "raw dataset");
    }

    @Test
    void mysqlBaselineMigrationIsSchemaOnlyAndExcludesDemoSeed() throws IOException {
        String migration = projectText(MYSQL_MIGRATION);

        assertThat(migration).contains(
                "Source baseline: src/main/resources/schema-mysql.sql",
                "Demo seed data from data-mysql.sql is intentionally not included",
                "CREATE TABLE IF NOT EXISTS tickets",
                "CREATE TABLE IF NOT EXISTS agent_runs",
                "CREATE TABLE IF NOT EXISTS tool_call_traces",
                "CREATE TABLE IF NOT EXISTS approval_requests",
                "CREATE TABLE IF NOT EXISTS orders",
                "CREATE TABLE IF NOT EXISTS aftersale_policies");
        assertThat(migration.toLowerCase(Locale.ROOT)).doesNotContain(
                "insert into",
                "api_key",
                "token=",
                "raw prompt",
                "raw dataset");
    }

    @Test
    void migrationDocsRecordSelectionBoundariesAndCompletion() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "Flyway migration foundation",
                "Liquibase is not introduced",
                "Liquibase 未引入",
                "spring.flyway.enabled: false",
                "AFTERSALE_FLYWAY_ENABLED:false",
                "AFTERSALE_RAG_FLYWAY_ENABLED:false",
                "classpath:db/migration/mysql",
                "classpath:db/migration/pgvector",
                "V5.B.2.3",
                "profile matrix validation harness",
                "TASK_COMPLETE");
    }

    @Test
    void migrationDocsKeepDefaultOfflineAndRuntimeNonChangeBoundary() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "Default Maven gate remains unchanged",
                "real LLM",
                "API Key",
                "PostgreSQL",
                "PGvector",
                "Docker",
                "MySQL",
                "Redis",
                "external network",
                "does not modify `src/main/java`",
                "ToolRegistry",
                "search_aftersale_policy",
                "RAG runtime");
    }

    @Test
    void docsDoNotContainSecretsLocalPathsOrOverclaims() throws IOException {
        for (String path : V5_B2_2_DOCS) {
            assertSafeText(path, projectText(path));
        }
        assertSafeText(MYSQL_MIGRATION, projectText(MYSQL_MIGRATION));
        assertSafeText(PGVECTOR_MIGRATION, projectText(PGVECTOR_MIGRATION));
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
        for (String path : V5_B2_2_DOCS) {
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
