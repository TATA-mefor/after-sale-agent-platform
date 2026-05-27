package com.example.aftersale.policy.rag.ingestion.domain;

public enum PolicyIngestionStatus {
    CREATED,
    RUNNING,
    CHUNKED,
    EMBEDDING,
    COMPLETED,
    FAILED,
    PARTIALLY_FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == PARTIALLY_FAILED || this == CANCELLED;
    }
}
