package com.example.aftersale.policy.rag.infrastructure.pgvector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.example.aftersale.policy.rag.domain.PolicyChunk;
import com.example.aftersale.policy.rag.domain.PolicyDocument;
import com.example.aftersale.policy.rag.domain.PolicyDocumentSourceType;
import com.example.aftersale.policy.rag.domain.PolicyEmbedding;
import com.example.aftersale.policy.rag.domain.VectorSearchMatch;
import com.example.aftersale.policy.rag.domain.VectorSearchQuery;
import com.example.aftersale.policy.rag.domain.VectorSearchResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Tag("live")
@EnabledIfSystemProperty(named = "live.rag", matches = "true")
class JdbcPolicyVectorRepositorySmokeTest {

    private static final int DIMENSIONS = 1536;
    private static final String ENV_URL = "AFTERSALE_PGVECTOR_URL";
    private static final String ENV_USERNAME = "AFTERSALE_PGVECTOR_USERNAME";
    private static final String ENV_PASSWORD = "AFTERSALE_PGVECTOR_PASSWORD";
    private static final String ENV_SCHEMA = "AFTERSALE_PGVECTOR_SCHEMA";
    private static final String MODEL = "v5-a3-fixed-vector";
    private static final String PREFIX = "v5a3-smoke-";
    private static final String SCHEMA_PATTERN = "[A-Za-z_][A-Za-z0-9_]*";
    private static final Instant NOW = Instant.parse("2026-06-01T00:00:00Z");
    private static final LocalDate EFFECTIVE_FROM = LocalDate.parse("2026-01-01");

    @Test
    void persistsAndSearchesPolicyEvidenceAgainstLivePgVector() throws IOException {
        SmokeSettings settings = SmokeSettings.fromEnvironment();
        DataSource dataSource = dataSource(settings);
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        JdbcPolicyVectorRepository repository = new JdbcPolicyVectorRepository(
                jdbcTemplate,
                new PgVectorProperties(
                        true,
                        settings.jdbcUrl(),
                        settings.username(),
                        settings.password(),
                        settings.schema(),
                        false,
                        DIMENSIONS));
        String idPrefix = PREFIX + UUID.randomUUID();

        initializeSchema(dataSource, settings.schema());
        cleanup(jdbcTemplate, settings.schema(), idPrefix);
        try {
            seed(repository, idPrefix);

            assertThat(repository.findDocumentById(idPrefix + "-doc-a"))
                    .isPresent()
                    .get()
                    .extracting(PolicyDocument::title)
                    .isEqualTo("Smoke return policy A");
            assertThat(repository.findChunkById(idPrefix + "-chunk-a")).isPresent();
            assertThat(repository.findEmbeddingByChunkIdAndModel(idPrefix + "-chunk-a", MODEL)).isPresent();

            VectorSearchResult result = repository.search(new VectorSearchQuery(
                    "fixed vector smoke query",
                    vector(1.0d, 0.0d),
                    3,
                    null,
                    "RETURN",
                    "electronics",
                    LocalDate.parse("2026-06-01"),
                    MODEL));

            assertThat(result.hasMatches()).isTrue();
            assertThat(result.matches()).extracting(VectorSearchMatch::documentId)
                    .containsExactly(idPrefix + "-doc-a", idPrefix + "-doc-b", idPrefix + "-doc-c");
            assertThat(result.matches().get(0).score()).isGreaterThan(result.matches().get(1).score());
            assertThat(result.matches().get(1).score()).isGreaterThan(result.matches().get(2).score());
            assertThat(result.matches()).allSatisfy(match -> assertThat(match.score()).isBetween(0.0d, 1.0d));

            assertThatThrownBy(() -> repository.saveDocument(document(idPrefix, "a", "checksum-a")))
                    .isInstanceOf(PgVectorRepositoryException.class)
                    .hasMessageContaining("duplicate key")
                    .hasMessageContaining("details are sanitized")
                    .satisfies(failure -> assertSafeFailureMessage(failure.getMessage()));
            assertThatThrownBy(() -> repository.search(new VectorSearchQuery(
                    "bad vector",
                    List.of(1.0d, 0.0d),
                    1,
                    null,
                    "RETURN",
                    "electronics",
                    null,
                    MODEL)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("vector dimensions mismatch")
                    .satisfies(failure -> assertSafeFailureMessage(failure.getMessage()));
        } finally {
            cleanup(jdbcTemplate, settings.schema(), idPrefix);
        }
    }

    private static void seed(JdbcPolicyVectorRepository repository, String idPrefix) {
        repository.saveDocument(document(idPrefix, "a", "checksum-a"));
        repository.saveDocument(document(idPrefix, "b", "checksum-b"));
        repository.saveDocument(document(idPrefix, "c", "checksum-c"));
        repository.saveChunk(chunk(idPrefix, "a", "Return policy smoke evidence A."));
        repository.saveChunk(chunk(idPrefix, "b", "Return policy smoke evidence B."));
        repository.saveChunk(chunk(idPrefix, "c", "Return policy smoke evidence C."));
        repository.saveEmbedding(embedding(idPrefix, "a", vector(1.0d, 0.0d)));
        repository.saveEmbedding(embedding(idPrefix, "b", vector(0.8d, 0.6d)));
        repository.saveEmbedding(embedding(idPrefix, "c", vector(0.0d, 1.0d)));
    }

    private static PolicyDocument document(String idPrefix, String suffix, String checksumSuffix) {
        return new PolicyDocument(
                idPrefix + "-doc-" + suffix,
                "Smoke return policy " + suffix.toUpperCase(Locale.ROOT),
                "RETURN",
                "electronics",
                "v5-a3",
                PolicyDocumentSourceType.MARKDOWN,
                "policy://v5-a3/" + suffix,
                idPrefix + "-" + checksumSuffix,
                EFFECTIVE_FROM,
                null,
                NOW,
                NOW);
    }

    private static PolicyChunk chunk(String idPrefix, String suffix, String content) {
        return new PolicyChunk(
                idPrefix + "-chunk-" + suffix,
                idPrefix + "-doc-" + suffix,
                0,
                content,
                16,
                "{\"source\":\"v5-a3-smoke\"}",
                NOW);
    }

    private static PolicyEmbedding embedding(String idPrefix, String suffix, List<Double> vector) {
        return new PolicyEmbedding(
                idPrefix + "-embedding-" + suffix,
                idPrefix + "-chunk-" + suffix,
                MODEL,
                DIMENSIONS,
                vector,
                NOW);
    }

    private static List<Double> vector(double first, double second) {
        List<Double> vector = new ArrayList<>(DIMENSIONS);
        vector.add(first);
        vector.add(second);
        for (int index = 2; index < DIMENSIONS; index++) {
            vector.add(0.0d);
        }
        return vector;
    }

    private static void initializeSchema(DataSource dataSource, String schema) throws IOException {
        String schemaSql = Files.readString(Path.of("src/main/resources/schema-rag-postgres.sql"),
                StandardCharsets.UTF_8);
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
            statement.execute("SET search_path TO " + schema);
            for (String sql : schemaSql.split(";")) {
                String statementSql = sql.trim();
                if (!statementSql.isBlank()) {
                    executeSchemaStatement(statement, statementSql);
                }
            }
        } catch (CannotGetJdbcConnectionException | SQLException exception) {
            throw sanitizedLiveFailure("Live PGvector smoke failed to connect or initialize schema");
        }
    }

    private static void executeSchemaStatement(Statement statement, String statementSql) throws SQLException {
        try {
            statement.execute(statementSql);
        } catch (SQLException exception) {
            if (statementSql.toLowerCase(Locale.ROOT).contains("create extension")) {
                assumeTrue(false, "PGvector extension setup is unavailable for this database user; "
                        + "use the docker-compose-rag init mount or preinstall the vector extension.");
            }
            throw exception;
        }
    }

    private static void cleanup(NamedParameterJdbcTemplate jdbcTemplate, String schema, String idPrefix) {
        try {
            jdbcTemplate.update(
                    "DELETE FROM " + schema + ".policy_documents WHERE document_id LIKE :prefix",
                    new MapSqlParameterSource().addValue("prefix", idPrefix + "%"));
        } catch (CannotGetJdbcConnectionException exception) {
            throw sanitizedLiveFailure("Live PGvector smoke cleanup failed to connect");
        }
    }

    private static DriverManagerDataSource dataSource(SmokeSettings settings) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(settings.jdbcUrl());
        dataSource.setUsername(settings.username());
        dataSource.setPassword(settings.password());
        return dataSource;
    }

    private static AssertionError sanitizedLiveFailure(String message) {
        return new AssertionError(message + "; details are sanitized.");
    }

    private static void assertSafeFailureMessage(String message) {
        assertThat(message).doesNotContain(
                "jdbc:postgresql",
                "password",
                "token",
                "secret",
                "[1",
                "Return policy smoke evidence");
    }

    private record SmokeSettings(String jdbcUrl, String username, String password, String schema) {

        private static SmokeSettings fromEnvironment() {
            String jdbcUrl = requiredEnv(ENV_URL);
            String username = requiredEnv(ENV_USERNAME);
            String password = requiredEnv(ENV_PASSWORD);
            String schema = optionalEnv(ENV_SCHEMA, "public");
            assumeTrue(schema.matches(SCHEMA_PATTERN), "AFTERSALE_PGVECTOR_SCHEMA must be a simple schema name.");
            return new SmokeSettings(jdbcUrl, username, password, schema);
        }

        private static String requiredEnv(String name) {
            String value = System.getenv(name);
            assumeTrue(value != null && !value.isBlank(), name + " is required for live PGvector smoke.");
            return value.trim();
        }

        private static String optionalEnv(String name, String fallback) {
            String value = System.getenv(name);
            if (value == null || value.isBlank()) {
                return fallback;
            }
            return value.trim();
        }
    }
}
