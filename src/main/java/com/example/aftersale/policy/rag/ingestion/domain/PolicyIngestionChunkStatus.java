package com.example.aftersale.policy.rag.ingestion.domain;

public enum PolicyIngestionChunkStatus {
    CREATED,
    CHUNKED,
    EMBEDDING,
    EMBEDDED,
    FAILED,
    SKIPPED
}
