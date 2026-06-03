package io.github.tatame.aftersale.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ProfileMatrixValidationTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    @Test
    void defaultProfileKeepsOfflineAndNoDatasourceBoundary() throws IOException {
        String application = projectText("src/main/resources/application.yml");

        assertThat(application).contains(
                "spring:",
                "flyway:",
                "enabled: false",
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
                "include: health",
                "show-details: never",
                "show-actuator: false",
                "enabled: ${AFTERSALE_RAG_ENABLED:false}",
                "provider: ${AFTERSALE_VECTOR_STORE_PROVIDER:none}",
                "enabled: ${AFTERSALE_PGVECTOR_ENABLED:false}",
                "enabled: ${SPRING_AI_ENABLED:false}",
                "chat-enabled: ${SPRING_AI_CHAT_ENABLED:false}",
                "embedding-enabled: ${SPRING_AI_EMBEDDING_ENABLED:false}");
    }

    @Test
    void mysqlProfileIsExplicitOptInAndDoesNotContainPgvectorVariables() throws IOException {
        String mysql = projectText("src/main/resources/application-mysql.yml");

        assertThat(mysql).contains(
                "on-profile: mysql",
                "enabled: ${AFTERSALE_FLYWAY_ENABLED:false}",
                "locations: classpath:db/migration/mysql",
                "AFTERSALE_MYSQL_URL",
                "AFTERSALE_MYSQL_USERNAME",
                "AFTERSALE_MYSQL_PASSWORD",
                "classpath:schema-mysql.sql",
                "classpath:data-mysql.sql");
        assertThat(mysql).doesNotContain(
                "AFTERSALE_PGVECTOR_URL",
                "AFTERSALE_PGVECTOR_USERNAME",
                "AFTERSALE_PGVECTOR_PASSWORD",
                "AFTERSALE_RAG_PGVECTOR_URL");
    }

    @Test
    void ragPostgresProfileUsesExistingPgvectorVariablesAndDisabledFlywayDefault() throws IOException {
        String ragPostgres = projectText("src/main/resources/application-rag-postgres.yml");

        assertThat(ragPostgres).contains(
                "on-profile: rag-postgres",
                "enabled: ${AFTERSALE_RAG_FLYWAY_ENABLED:false}",
                "locations: classpath:db/migration/pgvector",
                "enabled: ${AFTERSALE_RAG_ENABLED:true}",
                "provider: ${AFTERSALE_VECTOR_STORE_PROVIDER:pgvector}",
                "enabled: ${AFTERSALE_PGVECTOR_ENABLED:true}",
                "jdbc-url: ${AFTERSALE_PGVECTOR_URL:}",
                "username: ${AFTERSALE_PGVECTOR_USERNAME:}",
                "password: ${AFTERSALE_PGVECTOR_PASSWORD:}",
                "schema: ${AFTERSALE_PGVECTOR_SCHEMA:public}");
        assertThat(ragPostgres).doesNotContain(
                "AFTERSALE_RAG_PGVECTOR_URL",
                "AFTERSALE_RAG_PGVECTOR_USERNAME",
                "AFTERSALE_RAG_PGVECTOR_PASSWORD");
    }

    @Test
    void productionTemplateIsTemplateOnlyAndEnvironmentDriven() throws IOException {
        String prodTemplate = projectText("src/main/resources/application-prod.example.yml");

        assertThat(prodTemplate).contains(
                "Production profile template for AfterSale-Agent",
                "on-profile: prod",
                "SPRING_DATASOURCE_URL",
                "SPRING_DATASOURCE_USERNAME",
                "SPRING_DATASOURCE_PASSWORD",
                "AFTERSALE_OPENAPI_ENABLED",
                "AFTERSALE_SWAGGER_UI_ENABLED",
                "AFTERSALE_RAG_ENABLED:false",
                "AFTERSALE_PGVECTOR_ENABLED:false",
                "SPRING_AI_ENABLED:false",
                "This template does not enable production authentication");
        assertThat(prodTemplate.toLowerCase(Locale.ROOT)).doesNotContain(
                "production deployment completed",
                "production auth completed",
                "production monitoring completed",
                "secret manager completed");
    }

    @Test
    void migrationFilesExistAndRemainSchemaOnlyBaselines() throws IOException {
        String mysql = projectText("src/main/resources/db/migration/mysql/V20260603001__mysql_baseline.sql");
        String pgvector = projectText(
                "src/main/resources/db/migration/pgvector/"
                        + "V20260601001__pgvector_policy_rag_baseline.sql");

        assertThat(mysql).contains("Source baseline: src/main/resources/schema-mysql.sql");
        assertThat(pgvector).contains(
                "Source baseline: src/main/resources/schema-rag-postgres.sql",
                "CREATE EXTENSION IF NOT EXISTS vector");
        assertThat(mysql.toLowerCase(Locale.ROOT)).doesNotContain("insert into");
        assertThat(pgvector.toLowerCase(Locale.ROOT)).doesNotContain("insert into");
    }

    @Test
    void ciWorkflowKeepsDefaultOfflineGateWithoutLiveFlagsOrSecrets() throws IOException {
        String workflow = projectText(".github/workflows/ci.yml");
        String lower = workflow.toLowerCase(Locale.ROOT);

        assertThat(workflow).contains(
                "mvn -B --no-transfer-progress test",
                "mvn -B --no-transfer-progress checkstyle:check",
                "mvn -B --no-transfer-progress spotbugs:check",
                "mvn -B --no-transfer-progress test -Dtest=ArchitectureTest",
                "docker build -t after-sale-agent-platform:ci .");
        assertThat(lower).doesNotContain(
                "live.rag=true",
                "live.llm=true",
                "live.spring-ai=true",
                "live.embedding=true",
                "live.pgvector=true",
                "secrets.");
    }

    @Test
    void livePgvectorSmokeRemainsExplicitOptInWithExistingVariables() throws IOException {
        String smokeTest = projectText(
                "src/test/java/io/github/tatame/aftersale/policy/rag/infrastructure/pgvector/"
                        + "JdbcPolicyVectorRepositorySmokeTest.java");
        String envExample = projectText(".env.rag.example");

        assertThat(smokeTest).contains(
                "@Tag(\"live\")",
                "@EnabledIfSystemProperty(named = \"live.rag\", matches = \"true\")",
                "AFTERSALE_PGVECTOR_URL",
                "AFTERSALE_PGVECTOR_USERNAME",
                "AFTERSALE_PGVECTOR_PASSWORD",
                "AFTERSALE_PGVECTOR_SCHEMA",
                "create extension",
                "assumeTrue(false",
                "docker-compose-rag init mount");
        assertThat(smokeTest).doesNotContain("AFTERSALE_RAG_PGVECTOR_URL");
        assertThat(envExample).contains(
                "AFTERSALE_PGVECTOR_URL=jdbc:postgresql://localhost:5433/after_sale_agent_rag",
                "AFTERSALE_PGVECTOR_USERNAME=aftersale_rag",
                "AFTERSALE_PGVECTOR_PASSWORD=aftersale_rag",
                "AFTERSALE_PGVECTOR_SCHEMA=public");
    }

    private static String projectText(String path) throws IOException {
        Path file = PROJECT_ROOT.resolve(path);
        assertThat(Files.exists(file)).as(path + " should exist").isTrue();
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
