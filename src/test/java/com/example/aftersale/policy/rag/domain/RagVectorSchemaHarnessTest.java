package com.example.aftersale.policy.rag.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class RagVectorSchemaHarnessTest {

    private static final String SCHEMA_RESOURCE = "schema-rag-postgres.sql";

    @Test
    void schemaDefinesPgVectorPolicyTablesAndRelations() {
        String schema = schema();

        assertThat(schema).contains("CREATE EXTENSION IF NOT EXISTS vector");
        assertThat(schema).contains("CREATE TABLE IF NOT EXISTS policy_documents");
        assertThat(schema).contains("CREATE TABLE IF NOT EXISTS policy_chunks");
        assertThat(schema).contains("CREATE TABLE IF NOT EXISTS policy_embeddings");
        assertThat(schema).contains("document_id VARCHAR(64) PRIMARY KEY");
        assertThat(schema).contains("chunk_id VARCHAR(64) PRIMARY KEY");
        assertThat(schema).contains("embedding_id VARCHAR(64) PRIMARY KEY");
        assertThat(schema).contains("CONSTRAINT fk_policy_chunks_document");
        assertThat(schema).contains("CONSTRAINT fk_policy_embeddings_chunk");
        assertThat(schema).contains("CONSTRAINT uq_policy_documents_checksum_version");
        assertThat(schema).contains("CONSTRAINT uq_policy_chunks_document_index");
        assertThat(schema).contains("CONSTRAINT uq_policy_embeddings_chunk_model");
        assertThat(schema).contains("idx_policy_documents_category_product");
        assertThat(schema).contains("idx_policy_documents_effective_dates");
        assertThat(schema).contains("idx_policy_chunks_document");
        assertThat(schema).contains("idx_policy_embeddings_model");
        assertThat(schema).contains("embedding vector(1536) NOT NULL");
    }

    @Test
    void schemaDoesNotContainSecretsOrLocalPaths() {
        String schema = schema().toLowerCase();

        assertThat(schema).doesNotContain("api_key");
        assertThat(schema).doesNotContain("sk-");
        assertThat(schema).doesNotContain("password=");
        assertThat(schema).doesNotContain("jdbc:postgresql://prod");
        assertThat(schema).doesNotContain("d:\\");
        assertThat(schema).doesNotContain("c:\\");
        assertThat(schema).doesNotContain("/users/");
    }

    @Test
    void defaultAndRagProfileDoNotAutoLoadRagSchema() {
        String defaultConfig = resource("application.yml");
        String ragProfileConfig = resource("application-rag-postgres.yml");

        assertThat(defaultConfig).doesNotContain(SCHEMA_RESOURCE);
        assertThat(ragProfileConfig).doesNotContain(SCHEMA_RESOURCE);
        assertThat(defaultConfig).contains("initialize-schema: false");
        assertThat(ragProfileConfig).contains("initialize-schema: false");
    }

    private static String schema() {
        return resource(SCHEMA_RESOURCE);
    }

    private static String resource(String path) {
        try {
            return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
