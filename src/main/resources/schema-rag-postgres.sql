-- V5.A.2 schema version: 2026-06-01-001
-- Intended for: JdbcPolicyVectorRepository / PGvector policy evidence search
-- Migration framework: pending V5.B.2
-- Current initialization path: manual SQL import or docker-compose-rag init mount
-- Default test boundary: not loaded by default mvn test
-- V4.3.2 opt-in PGvector schema contract.
-- This file is for the rag-postgres profile only and is not loaded by the default test profile.
-- The vector column uses 1536 as the default OpenAI-compatible embedding dimension. Runtime dimensions remain
-- configuration-driven through AFTERSALE_EMBEDDING_DIMENSION for later V4 phases.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS policy_documents (
    document_id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    product_type VARCHAR(64) NOT NULL,
    version VARCHAR(64) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_uri VARCHAR(1024),
    checksum VARCHAR(128) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_policy_documents_checksum_version UNIQUE (checksum, version),
    CONSTRAINT ck_policy_documents_effective_dates CHECK (
        effective_to IS NULL OR effective_to >= effective_from
    )
);

CREATE TABLE IF NOT EXISTS policy_chunks (
    chunk_id VARCHAR(64) PRIMARY KEY,
    document_id VARCHAR(64) NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    token_estimate INTEGER NOT NULL,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_policy_chunks_document
        FOREIGN KEY (document_id)
        REFERENCES policy_documents (document_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_policy_chunks_document_index UNIQUE (document_id, chunk_index),
    CONSTRAINT ck_policy_chunks_chunk_index CHECK (chunk_index >= 0),
    CONSTRAINT ck_policy_chunks_token_estimate CHECK (token_estimate >= 0)
);

CREATE TABLE IF NOT EXISTS policy_embeddings (
    embedding_id VARCHAR(64) PRIMARY KEY,
    chunk_id VARCHAR(64) NOT NULL,
    embedding_model VARCHAR(128) NOT NULL,
    embedding_dimension INTEGER NOT NULL,
    embedding vector(1536) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_policy_embeddings_chunk
        FOREIGN KEY (chunk_id)
        REFERENCES policy_chunks (chunk_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_policy_embeddings_chunk_model UNIQUE (chunk_id, embedding_model),
    CONSTRAINT ck_policy_embeddings_dimension CHECK (embedding_dimension > 0)
);

CREATE INDEX IF NOT EXISTS idx_policy_documents_category_product
    ON policy_documents (category, product_type);

CREATE INDEX IF NOT EXISTS idx_policy_documents_effective_dates
    ON policy_documents (effective_from, effective_to);

CREATE INDEX IF NOT EXISTS idx_policy_documents_checksum
    ON policy_documents (checksum);

CREATE INDEX IF NOT EXISTS idx_policy_chunks_document
    ON policy_chunks (document_id);

CREATE INDEX IF NOT EXISTS idx_policy_embeddings_chunk
    ON policy_embeddings (chunk_id);

CREATE INDEX IF NOT EXISTS idx_policy_embeddings_model
    ON policy_embeddings (embedding_model);

-- Optional for later V4.3.x live PGvector search after representative data volume exists:
-- CREATE INDEX IF NOT EXISTS idx_policy_embeddings_embedding_hnsw
--     ON policy_embeddings USING hnsw (embedding vector_cosine_ops);
