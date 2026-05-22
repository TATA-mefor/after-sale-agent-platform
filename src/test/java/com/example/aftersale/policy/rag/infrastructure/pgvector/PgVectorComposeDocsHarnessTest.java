package com.example.aftersale.policy.rag.infrastructure.pgvector;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PgVectorComposeDocsHarnessTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    @Test
    void ragComposeDefinesOptInPgVectorService() throws IOException {
        String compose = projectText("docker-compose-rag.yml");

        assertThat(compose).contains("pgvector:");
        assertThat(compose).contains("pgvector/pgvector:pg16");
        assertThat(compose).contains("aftersale_pgvector_data");
        assertThat(compose).contains("healthcheck:");
        assertThat(compose).contains("pg_isready");
        assertThat(compose).contains("schema-rag-postgres.sql");
        assertThat(compose).contains("docker-entrypoint-initdb.d/01-schema-rag-postgres.sql");
        assertThat(compose).doesNotContain("app:");
        assertThat(compose).doesNotContain("depends_on");
        assertThat(compose).doesNotContain("SPRING_PROFILES_ACTIVE");
    }

    @Test
    void ragComposeAndEnvExampleUseOnlyPlaceholderLocalCredentials() throws IOException {
        String compose = projectText("docker-compose-rag.yml");
        String envExample = projectText(".env.rag.example");

        assertSecretSafe(compose);
        assertSecretSafe(envExample);
        assertThat(envExample).contains("AFTERSALE_RAG_ENABLED=true");
        assertThat(envExample).contains("AFTERSALE_VECTOR_STORE_PROVIDER=pgvector");
        assertThat(envExample).contains("AFTERSALE_PGVECTOR_ENABLED=true");
        assertThat(envExample).contains("AFTERSALE_PGVECTOR_URL=jdbc:postgresql://localhost:5433/after_sale_agent_rag");
        assertThat(envExample).contains("AFTERSALE_PGVECTOR_USERNAME=aftersale_rag");
        assertThat(envExample).contains("AFTERSALE_PGVECTOR_PASSWORD=aftersale_rag");
        assertThat(envExample).contains("AFTERSALE_PGVECTOR_SCHEMA=public");
        assertThat(envExample).contains("AFTERSALE_EMBEDDING_DIMENSION=1536");
        assertThat(envExample).contains("jdbc:postgresql://pgvector:5432/after_sale_agent_rag");
    }

    @Test
    void defaultComposePathIsNotPollutedByPgVector() throws IOException {
        String compose = projectText("docker-compose.yml");

        assertThat(compose).contains("mysql:");
        assertThat(compose).contains("app:");
        assertThat(compose).contains("mysql:8.0");
        assertThat(compose).doesNotContain("pgvector");
        assertThat(compose).doesNotContain("rag-postgres");
        assertThat(compose).doesNotContain("AFTERSALE_PGVECTOR");
        assertThat(compose).doesNotContain("jdbc:postgresql:");
        assertThat(compose).doesNotContain("schema-rag-postgres.sql");
    }

    @Test
    void pgVectorDocsDescribeOptInBoundaryAndKnownNonGoals() throws IOException {
        String readme = projectText("README.md");
        String setupDoc = projectText("docs/demo/V4_PGVECTOR_LOCAL_SETUP.md");
        String docs = readme + "\n" + setupDoc;

        assertThat(docs).contains("opt-in");
        assertThat(docs).contains("local development only");
        assertThat(docs).contains("default `docker-compose.yml` app + MySQL path does not depend on PGvector");
        assertThat(docs).contains("Default validation does not start Docker");
        assertThat(docs).contains("No `JdbcPolicyVectorRepository` yet");
        assertThat(docs).contains("No Policy Ingestion yet");
        assertThat(docs).contains("No HYBRID retrieval yet");
        assertThat(docs).contains("`search_aftersale_policy` is not wired to vector search yet");
        assertThat(docs).contains("docker compose -f docker-compose-rag.yml up -d");
        assertThat(docs).contains("docker compose -f docker-compose-rag.yml down");
        assertThat(docs).contains("docker compose -f docker-compose-rag.yml down -v");
        assertThat(docs).contains("AFTERSALE_PGVECTOR_URL");
        assertThat(docs).contains("AFTERSALE_PGVECTOR_USERNAME");
        assertThat(docs).contains("AFTERSALE_PGVECTOR_PASSWORD");
        assertSecretSafe(setupDoc);
    }

    private static String projectText(String path) throws IOException {
        Path file = PROJECT_ROOT.resolve(path);
        assertThat(Files.exists(file)).as(path + " should exist").isTrue();
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private static void assertSecretSafe(String text) {
        String lower = text.toLowerCase();

        assertThat(lower).doesNotContain("openai_api_key");
        assertThat(lower).doesNotContain("sk-");
        assertThat(lower).doesNotContain("api_key=");
        assertThat(lower).doesNotContain("jdbc:postgresql://prod");
        assertThat(lower).doesNotContain("jdbc:postgresql://production");
        assertThat(lower).doesNotContain("d:\\");
        assertThat(lower).doesNotContain("c:\\");
        assertThat(lower).doesNotContain("/users/");
    }
}
