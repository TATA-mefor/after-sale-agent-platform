package io.github.tatame.aftersale.policy.rag.infrastructure.pgvector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.tatame.aftersale.policy.rag.domain.PolicyChunk;
import io.github.tatame.aftersale.policy.rag.domain.PolicyDocument;
import io.github.tatame.aftersale.policy.rag.domain.PolicyDocumentSourceType;
import io.github.tatame.aftersale.policy.rag.domain.PolicyEmbedding;
import io.github.tatame.aftersale.policy.rag.domain.VectorSearchMatch;
import io.github.tatame.aftersale.policy.rag.domain.VectorSearchQuery;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

class JdbcPolicyVectorRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-05-22T00:00:00Z");

    @Test
    void formatsVectorLiteralAndRejectsDimensionOrNonFiniteValues() {
        assertThat(JdbcPolicyVectorRepository.toVectorLiteral(List.of(1.0d, 0.25d, -2.5d), 3))
                .isEqualTo("[1,0.25,-2.5]");

        assertThatThrownBy(() -> JdbcPolicyVectorRepository.toVectorLiteral(List.of(1.0d, 2.0d), 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vector dimensions mismatch");
        assertThatThrownBy(() -> JdbcPolicyVectorRepository.toVectorLiteral(List.of(1.0d, Double.NaN), 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite numbers");
    }

    @Test
    void saveEmbeddingUsesPgVectorCastAndParameterizedVector() {
        NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        JdbcPolicyVectorRepository repository = repository(jdbcOperations, "rag", 3);
        PolicyEmbedding embedding = new PolicyEmbedding(
                "embedding-1",
                "chunk-1",
                "fake-embedding",
                3,
                List.of(0.1d, 0.2d, 0.3d),
                NOW);

        repository.saveEmbedding(embedding);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> parametersCaptor =
                ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcOperations).update(sqlCaptor.capture(), parametersCaptor.capture());
        assertThat(sqlCaptor.getValue())
                .contains("INSERT INTO rag.policy_embeddings")
                .contains("CAST(:embedding AS vector)")
                .doesNotContain("[0.1,0.2,0.3]");
        assertThat(parametersCaptor.getValue().getValue("embedding")).isEqualTo("[0.1,0.2,0.3]");
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchUsesPlaceholdersAndDoesNotInlineVectorOrFilters() {
        NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        when(jdbcOperations.query(
                anyString(),
                any(MapSqlParameterSource.class),
                any(RowMapper.class)))
                .thenReturn(List.of());
        JdbcPolicyVectorRepository repository = repository(jdbcOperations, "rag", 3);
        VectorSearchQuery query = new VectorSearchQuery(
                "refund policy",
                List.of(0.1d, 0.2d, 0.3d),
                5,
                0.5d,
                "RETURN",
                "electronics",
                LocalDate.parse("2026-05-22"),
                "fake-embedding");

        repository.search(query);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> parametersCaptor =
                ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcOperations).query(sqlCaptor.capture(), parametersCaptor.capture(), any(RowMapper.class));
        assertThat(sqlCaptor.getValue())
                .contains("e.embedding <=> CAST(:queryVector AS vector)")
                .contains("d.category = :category")
                .contains("d.product_type = :productType")
                .contains("LIMIT :topK")
                .doesNotContain("[0.1,0.2,0.3]")
                .doesNotContain("RETURN")
                .doesNotContain("electronics");
        assertThat(parametersCaptor.getValue().getValue("queryVector")).isEqualTo("[0.1,0.2,0.3]");
        assertThat(parametersCaptor.getValue().getValue("topK")).isEqualTo(5);
    }

    @Test
    void duplicateAndDataAccessFailuresExposeOnlySanitizedMessages() {
        NamedParameterJdbcOperations duplicateJdbc = mock(NamedParameterJdbcOperations.class);
        when(duplicateJdbc.update(anyString(), any(MapSqlParameterSource.class)))
                .thenThrow(new DuplicateKeyException("secret-value jdbc:postgresql://db [0.1,0.2]"));
        Throwable duplicateFailure = catchThrowable(() -> repository(duplicateJdbc, "rag", 3)
                .saveDocument(document()));

        assertThat(duplicateFailure)
                .isInstanceOf(PgVectorRepositoryException.class)
                .hasMessageContaining("duplicate key")
                .hasMessageContaining("details are sanitized")
                .hasNoCause();
        assertThat(duplicateFailure.getMessage())
                .doesNotContain("secret-value")
                .doesNotContain("jdbc:postgresql")
                .doesNotContain("[0.1,0.2]");

        NamedParameterJdbcOperations searchJdbc = mock(NamedParameterJdbcOperations.class);
        when(searchJdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenThrow(new DataAccessResourceFailureException("password=secret token=value"));
        Throwable searchFailure = catchThrowable(() -> repository(searchJdbc, "rag", 3)
                .search(new VectorSearchQuery(
                        "policy",
                        List.of(0.1d, 0.2d, 0.3d),
                        3,
                        null,
                        null,
                        null,
                        null,
                        "fake-embedding")));

        assertThat(searchFailure)
                .isInstanceOf(PgVectorRepositoryException.class)
                .hasMessageContaining("database access")
                .hasNoCause();
        assertThat(searchFailure.getMessage())
                .doesNotContain("password=")
                .doesNotContain("token=");
    }

    @Test
    void rowMappersReadDocumentsChunksEmbeddingsAndSearchMatches() throws SQLException {
        PolicyDocument document = JdbcPolicyVectorRepository.mapDocument(documentResultSet(), 0);
        PolicyChunk chunk = JdbcPolicyVectorRepository.mapChunk(chunkResultSet(), 0);
        PolicyEmbedding embedding = JdbcPolicyVectorRepository.mapEmbedding(embeddingResultSet(), 0);
        VectorSearchMatch match = JdbcPolicyVectorRepository.mapSearchMatch(searchResultSet(), 0);

        assertThat(document.documentId()).isEqualTo("doc-1");
        assertThat(document.sourceType()).isEqualTo(PolicyDocumentSourceType.MARKDOWN);
        assertThat(chunk.metadataJson()).isEqualTo("{\"source\":\"demo\"}");
        assertThat(embedding.vector()).containsExactly(0.1d, 0.2d, 0.3d);
        assertThat(match.documentId()).isEqualTo("doc-1");
        assertThat(match.score()).isEqualTo(0.81d);
        assertThat(match.distance()).isEqualTo(0.19d);
    }

    @Test
    void unsafeSchemaIsRejectedWithoutEchoingInput() {
        assertThatThrownBy(() -> repository(mock(NamedParameterJdbcOperations.class), "rag;drop table", 3))
                .isInstanceOf(PgVectorConfigurationException.class)
                .hasMessageContaining("schema name is invalid")
                .hasMessageNotContaining("rag;drop");
    }

    private static JdbcPolicyVectorRepository repository(
            NamedParameterJdbcOperations jdbcOperations,
            String schema,
            int dimensions) {
        return new JdbcPolicyVectorRepository(
                jdbcOperations,
                new PgVectorProperties(true, "jdbc:postgresql://localhost:5432/rag", "rag", "local", schema,
                        false, dimensions));
    }

    private static PolicyDocument document() {
        return new PolicyDocument(
                "doc-1",
                "Return Policy",
                "RETURN",
                "electronics",
                "v1",
                PolicyDocumentSourceType.MARKDOWN,
                "policy://doc-1",
                "checksum-doc-1",
                LocalDate.parse("2026-01-01"),
                null,
                NOW,
                NOW);
    }

    private static ResultSet documentResultSet() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("document_id")).thenReturn("doc-1");
        when(resultSet.getString("title")).thenReturn("Return Policy");
        when(resultSet.getString("category")).thenReturn("RETURN");
        when(resultSet.getString("product_type")).thenReturn("electronics");
        when(resultSet.getString("version")).thenReturn("v1");
        when(resultSet.getString("source_type")).thenReturn("MARKDOWN");
        when(resultSet.getString("source_uri")).thenReturn("policy://doc-1");
        when(resultSet.getString("checksum")).thenReturn("checksum-doc-1");
        when(resultSet.getObject("effective_from")).thenReturn(Date.valueOf("2026-01-01"));
        when(resultSet.getObject("effective_to")).thenReturn(null);
        when(resultSet.getObject("created_at")).thenReturn(Timestamp.from(NOW));
        when(resultSet.getObject("updated_at")).thenReturn(Timestamp.from(NOW));
        return resultSet;
    }

    private static ResultSet chunkResultSet() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("chunk_id")).thenReturn("chunk-1");
        when(resultSet.getString("document_id")).thenReturn("doc-1");
        when(resultSet.getInt("chunk_index")).thenReturn(0);
        when(resultSet.getString("content")).thenReturn("Return policy evidence.");
        when(resultSet.getInt("token_estimate")).thenReturn(12);
        when(resultSet.getString("metadata_json")).thenReturn("{\"source\":\"demo\"}");
        when(resultSet.getObject("created_at")).thenReturn(Timestamp.from(NOW));
        return resultSet;
    }

    private static ResultSet embeddingResultSet() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("embedding_id")).thenReturn("embedding-1");
        when(resultSet.getString("chunk_id")).thenReturn("chunk-1");
        when(resultSet.getString("embedding_model")).thenReturn("fake-embedding");
        when(resultSet.getInt("embedding_dimension")).thenReturn(3);
        when(resultSet.getString("embedding_vector")).thenReturn("[0.1,0.2,0.3]");
        when(resultSet.getObject("created_at")).thenReturn(Timestamp.from(NOW));
        return resultSet;
    }

    private static ResultSet searchResultSet() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("document_id")).thenReturn("doc-1");
        when(resultSet.getString("chunk_id")).thenReturn("chunk-1");
        when(resultSet.getString("document_title")).thenReturn("Return Policy");
        when(resultSet.getString("category")).thenReturn("RETURN");
        when(resultSet.getString("product_type")).thenReturn("electronics");
        when(resultSet.getString("snippet")).thenReturn("Return policy evidence.");
        when(resultSet.getDouble("score")).thenReturn(0.81d);
        when(resultSet.getDouble("distance")).thenReturn(0.19d);
        when(resultSet.getString("embedding_model")).thenReturn("fake-embedding");
        when(resultSet.getString("metadata_json")).thenReturn("{\"source\":\"demo\"}");
        return resultSet;
    }
}
