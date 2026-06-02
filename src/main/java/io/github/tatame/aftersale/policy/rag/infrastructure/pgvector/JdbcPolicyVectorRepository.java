package io.github.tatame.aftersale.policy.rag.infrastructure.pgvector;

import io.github.tatame.aftersale.policy.rag.domain.PolicyChunk;
import io.github.tatame.aftersale.policy.rag.domain.PolicyDocument;
import io.github.tatame.aftersale.policy.rag.domain.PolicyDocumentSourceType;
import io.github.tatame.aftersale.policy.rag.domain.PolicyEmbedding;
import io.github.tatame.aftersale.policy.rag.domain.PolicyVectorRepository;
import io.github.tatame.aftersale.policy.rag.domain.VectorSearchMatch;
import io.github.tatame.aftersale.policy.rag.domain.VectorSearchQuery;
import io.github.tatame.aftersale.policy.rag.domain.VectorSearchResult;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * Opt-in PostgreSQL / PGvector implementation of the policy vector repository contract.
 */
public final class JdbcPolicyVectorRepository implements PolicyVectorRepository {

    static final String DOCUMENTS_TABLE = "policy_documents";
    static final String CHUNKS_TABLE = "policy_chunks";
    static final String EMBEDDINGS_TABLE = "policy_embeddings";
    static final int SNIPPET_LIMIT = 240;

    private static final String SCHEMA_PATTERN = "[A-Za-z_][A-Za-z0-9_]*";
    private static final String VECTOR_DISTANCE_EXPRESSION = "e.embedding <=> CAST(:queryVector AS vector)";
    private static final String VECTOR_SCORE_EXPRESSION = "1.0 - (" + VECTOR_DISTANCE_EXPRESSION + ")";

    private final NamedParameterJdbcOperations jdbcOperations;
    private final String documentsTable;
    private final String chunksTable;
    private final String embeddingsTable;
    private final int expectedDimensions;

    public JdbcPolicyVectorRepository(
            NamedParameterJdbcOperations jdbcOperations,
            PgVectorProperties properties) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "jdbcOperations must not be null");
        PgVectorProperties normalized = Objects.requireNonNull(properties, "properties must not be null");
        this.expectedDimensions = normalized.dimensions();
        String schema = sanitizeSchema(normalized.schema());
        this.documentsTable = qualifiedTable(schema, DOCUMENTS_TABLE);
        this.chunksTable = qualifiedTable(schema, CHUNKS_TABLE);
        this.embeddingsTable = qualifiedTable(schema, EMBEDDINGS_TABLE);
    }

    @Override
    public PolicyDocument saveDocument(PolicyDocument document) {
        Objects.requireNonNull(document, "document must not be null");
        String sql = """
                INSERT INTO ${table} (
                    document_id, title, category, product_type, version, source_type, source_uri, checksum,
                    effective_from, effective_to, created_at, updated_at
                ) VALUES (
                    :documentId, :title, :category, :productType, :version, :sourceType, :sourceUri, :checksum,
                    :effectiveFrom, :effectiveTo, :createdAt, :updatedAt
                )
                """.replace("${table}", documentsTable);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("documentId", document.documentId())
                .addValue("title", document.title())
                .addValue("category", document.category())
                .addValue("productType", document.productType())
                .addValue("version", document.version())
                .addValue("sourceType", document.sourceType().name())
                .addValue("sourceUri", document.sourceUri())
                .addValue("checksum", document.checksum())
                .addValue("effectiveFrom", document.effectiveFrom())
                .addValue("effectiveTo", document.effectiveTo())
                .addValue("createdAt", Timestamp.from(document.createdAt()))
                .addValue("updatedAt", Timestamp.from(document.updatedAt()));
        update(sql, parameters, "save policy document");
        return document;
    }

    @Override
    public PolicyChunk saveChunk(PolicyChunk chunk) {
        Objects.requireNonNull(chunk, "chunk must not be null");
        String sql = """
                INSERT INTO ${table} (
                    chunk_id, document_id, chunk_index, content, token_estimate, metadata_json, created_at
                ) VALUES (
                    :chunkId, :documentId, :chunkIndex, :content, :tokenEstimate,
                    CAST(:metadataJson AS jsonb), :createdAt
                )
                """.replace("${table}", chunksTable);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("chunkId", chunk.chunkId())
                .addValue("documentId", chunk.documentId())
                .addValue("chunkIndex", chunk.chunkIndex())
                .addValue("content", chunk.content())
                .addValue("tokenEstimate", chunk.tokenEstimate())
                .addValue("metadataJson", chunk.metadataJson())
                .addValue("createdAt", Timestamp.from(chunk.createdAt()));
        update(sql, parameters, "save policy chunk");
        return chunk;
    }

    @Override
    public PolicyEmbedding saveEmbedding(PolicyEmbedding embedding) {
        Objects.requireNonNull(embedding, "embedding must not be null");
        String vectorLiteral = toVectorLiteral(embedding.vector(), expectedDimensions);
        String sql = """
                INSERT INTO ${table} (
                    embedding_id, chunk_id, embedding_model, embedding_dimension, embedding, created_at
                ) VALUES (
                    :embeddingId, :chunkId, :embeddingModel, :embeddingDimension,
                    CAST(:embedding AS vector), :createdAt
                )
                """.replace("${table}", embeddingsTable);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("embeddingId", embedding.embeddingId())
                .addValue("chunkId", embedding.chunkId())
                .addValue("embeddingModel", embedding.embeddingModel())
                .addValue("embeddingDimension", embedding.embeddingDimension())
                .addValue("embedding", vectorLiteral)
                .addValue("createdAt", Timestamp.from(embedding.createdAt()));
        update(sql, parameters, "save policy embedding");
        return embedding;
    }

    @Override
    public Optional<PolicyDocument> findDocumentById(String documentId) {
        String sql = """
                SELECT document_id, title, category, product_type, version, source_type, source_uri, checksum,
                       effective_from, effective_to, created_at, updated_at
                  FROM ${table}
                 WHERE document_id = :documentId
                """.replace("${table}", documentsTable);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("documentId", documentId);
        return queryForOptional(sql, parameters, JdbcPolicyVectorRepository::mapDocument, "find policy document");
    }

    @Override
    public Optional<PolicyChunk> findChunkById(String chunkId) {
        String sql = """
                SELECT chunk_id, document_id, chunk_index, content, token_estimate,
                       metadata_json::text AS metadata_json, created_at
                  FROM ${table}
                 WHERE chunk_id = :chunkId
                """.replace("${table}", chunksTable);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("chunkId", chunkId);
        return queryForOptional(sql, parameters, JdbcPolicyVectorRepository::mapChunk, "find policy chunk");
    }

    @Override
    public List<PolicyChunk> findChunksByDocumentId(String documentId) {
        String sql = """
                SELECT chunk_id, document_id, chunk_index, content, token_estimate,
                       metadata_json::text AS metadata_json, created_at
                  FROM ${table}
                 WHERE document_id = :documentId
                 ORDER BY chunk_index ASC, chunk_id ASC
                """.replace("${table}", chunksTable);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("documentId", documentId);
        return queryForList(sql, parameters, JdbcPolicyVectorRepository::mapChunk, "find policy chunks");
    }

    @Override
    public Optional<PolicyEmbedding> findEmbeddingByChunkIdAndModel(String chunkId, String embeddingModel) {
        String sql = """
                SELECT embedding_id, chunk_id, embedding_model, embedding_dimension,
                       embedding::text AS embedding_vector, created_at
                  FROM ${table}
                 WHERE chunk_id = :chunkId
                   AND embedding_model = :embeddingModel
                """.replace("${table}", embeddingsTable);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("chunkId", chunkId)
                .addValue("embeddingModel", embeddingModel);
        return queryForOptional(sql, parameters, JdbcPolicyVectorRepository::mapEmbedding, "find policy embedding");
    }

    @Override
    public VectorSearchResult search(VectorSearchQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        String vectorLiteral = toVectorLiteral(query.queryVector(), expectedDimensions);
        StringBuilder sql = new StringBuilder();
        sql.append("""
                SELECT d.document_id, c.chunk_id, d.title AS document_title, d.category, d.product_type,
                       CASE
                           WHEN length(c.content) <= :snippetLimit THEN c.content
                           ELSE trim(substring(c.content from 1 for :snippetLimit))
                       END AS snippet,
                       """);
        sql.append(VECTOR_SCORE_EXPRESSION)
                .append(" AS score, ")
                .append(VECTOR_DISTANCE_EXPRESSION)
                .append("""
                         AS distance,
                       e.embedding_model, c.metadata_json::text AS metadata_json
                  FROM """)
                .append(embeddingsTable)
                .append(" e JOIN ")
                .append(chunksTable)
                .append(" c ON c.chunk_id = e.chunk_id JOIN ")
                .append(documentsTable)
                .append("""
                 d ON d.document_id = c.document_id
                 WHERE (:embeddingModel IS NULL OR e.embedding_model = :embeddingModel)
                """);
        appendSearchFilters(sql, query);
        sql.append(" ORDER BY score DESC, d.document_id ASC, c.chunk_id ASC LIMIT :topK");
        MapSqlParameterSource parameters = searchParameters(query, vectorLiteral);
        List<VectorSearchMatch> matches = queryForList(
                sql.toString(), parameters, JdbcPolicyVectorRepository::mapSearchMatch, "search policy embeddings");
        if (matches.isEmpty()) {
            return VectorSearchResult.empty("No policy evidence matches found in PGvector repository.", false);
        }
        return VectorSearchResult.matched(matches);
    }

    static String toVectorLiteral(List<Double> vector, int expectedDimensions) {
        Objects.requireNonNull(vector, "vector must not be null");
        if (expectedDimensions <= 0) {
            throw new IllegalArgumentException("expected vector dimensions must be greater than zero");
        }
        if (vector.size() != expectedDimensions) {
            throw new IllegalArgumentException("vector dimensions mismatch: expected "
                    + expectedDimensions + ", actual " + vector.size());
        }
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (Double value : vector) {
            if (value == null || !Double.isFinite(value)) {
                throw new IllegalArgumentException("vector values must be finite numbers");
            }
            joiner.add(BigDecimal.valueOf(value).stripTrailingZeros().toPlainString());
        }
        return joiner.toString();
    }

    static List<Double> parseVectorLiteral(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String normalized = value.trim();
        if (!normalized.startsWith("[") || !normalized.endsWith("]")) {
            throw new IllegalArgumentException("PGvector value must use bracket literal format");
        }
        String body = normalized.substring(1, normalized.length() - 1).trim();
        if (body.isBlank()) {
            return List.of();
        }
        String[] parts = body.split(",");
        List<Double> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            double number = Double.parseDouble(part.trim());
            if (!Double.isFinite(number)) {
                throw new IllegalArgumentException("PGvector values must be finite numbers");
            }
            result.add(number);
        }
        return result;
    }

    static PolicyDocument mapDocument(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PolicyDocument(
                resultSet.getString("document_id"),
                resultSet.getString("title"),
                resultSet.getString("category"),
                resultSet.getString("product_type"),
                resultSet.getString("version"),
                PolicyDocumentSourceType.valueOf(resultSet.getString("source_type")),
                resultSet.getString("source_uri"),
                resultSet.getString("checksum"),
                toLocalDate(resultSet.getObject("effective_from")),
                toNullableLocalDate(resultSet.getObject("effective_to")),
                toInstant(resultSet.getObject("created_at")),
                toInstant(resultSet.getObject("updated_at")));
    }

    static PolicyChunk mapChunk(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PolicyChunk(
                resultSet.getString("chunk_id"),
                resultSet.getString("document_id"),
                resultSet.getInt("chunk_index"),
                resultSet.getString("content"),
                resultSet.getInt("token_estimate"),
                resultSet.getString("metadata_json"),
                toInstant(resultSet.getObject("created_at")));
    }

    static PolicyEmbedding mapEmbedding(ResultSet resultSet, int rowNumber) throws SQLException {
        List<Double> vector = parseVectorLiteral(resultSet.getString("embedding_vector"));
        return new PolicyEmbedding(
                resultSet.getString("embedding_id"),
                resultSet.getString("chunk_id"),
                resultSet.getString("embedding_model"),
                resultSet.getInt("embedding_dimension"),
                vector,
                toInstant(resultSet.getObject("created_at")));
    }

    static VectorSearchMatch mapSearchMatch(ResultSet resultSet, int rowNumber) throws SQLException {
        return new VectorSearchMatch(
                resultSet.getString("document_id"),
                resultSet.getString("chunk_id"),
                resultSet.getString("document_title"),
                resultSet.getString("category"),
                resultSet.getString("product_type"),
                resultSet.getString("snippet"),
                resultSet.getDouble("score"),
                resultSet.getDouble("distance"),
                resultSet.getString("embedding_model"),
                resultSet.getString("metadata_json"));
    }

    private void update(String sql, MapSqlParameterSource parameters, String operation) {
        try {
            jdbcOperations.update(sql, parameters);
        } catch (DuplicateKeyException exception) {
            throw sanitizedFailure(operation, "duplicate key");
        } catch (DataAccessException exception) {
            throw sanitizedFailure(operation, "database access");
        }
    }

    private <T> Optional<T> queryForOptional(
            String sql,
            MapSqlParameterSource parameters,
            RowMapper<T> rowMapper,
            String operation) {
        try {
            return Optional.ofNullable(jdbcOperations.queryForObject(sql, parameters, rowMapper));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        } catch (DataAccessException exception) {
            throw sanitizedFailure(operation, "database access");
        }
    }

    private <T> List<T> queryForList(
            String sql,
            MapSqlParameterSource parameters,
            RowMapper<T> rowMapper,
            String operation) {
        try {
            return jdbcOperations.query(sql, parameters, rowMapper);
        } catch (DataAccessException exception) {
            throw sanitizedFailure(operation, "database access");
        }
    }

    private PgVectorRepositoryException sanitizedFailure(
            String operation,
            String reason) {
        return new PgVectorRepositoryException("PGvector repository failed to " + operation
                + " due to " + reason + " error; details are sanitized.");
    }

    private static void appendSearchFilters(StringBuilder sql, VectorSearchQuery query) {
        if (query.category() != null) {
            sql.append(" AND d.category = :category");
        }
        if (query.productType() != null) {
            sql.append(" AND d.product_type = :productType");
        }
        if (query.effectiveAt() != null) {
            sql.append(" AND d.effective_from <= :effectiveAt")
                    .append(" AND (d.effective_to IS NULL OR d.effective_to >= :effectiveAt)");
        }
        if (query.minScore() != null) {
            sql.append(" AND ")
                    .append(VECTOR_SCORE_EXPRESSION)
                    .append(" >= :minScore");
        }
    }

    private static MapSqlParameterSource searchParameters(VectorSearchQuery query, String vectorLiteral) {
        return new MapSqlParameterSource()
                .addValue("queryVector", vectorLiteral)
                .addValue("snippetLimit", SNIPPET_LIMIT)
                .addValue("embeddingModel", query.embeddingModel())
                .addValue("category", query.category())
                .addValue("productType", query.productType())
                .addValue("effectiveAt", query.effectiveAt())
                .addValue("minScore", query.minScore())
                .addValue("topK", query.topK());
    }

    private static String sanitizeSchema(String schema) {
        String normalized = schema == null || schema.isBlank() ? "public" : schema.trim();
        if (!normalized.matches(SCHEMA_PATTERN)) {
            throw new PgVectorConfigurationException("PGvector schema name is invalid");
        }
        return normalized;
    }

    private static String qualifiedTable(String schema, String table) {
        return schema + "." + table;
    }

    private static LocalDate toLocalDate(Object value) {
        LocalDate result = toNullableLocalDate(value);
        if (result == null) {
            throw new IllegalArgumentException("date value must not be null");
        }
        return result;
    }

    private static LocalDate toNullableLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        throw new IllegalArgumentException("unsupported date value type");
    }

    private static Instant toInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        throw new IllegalArgumentException("unsupported timestamp value type");
    }
}
