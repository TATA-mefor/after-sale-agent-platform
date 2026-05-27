package com.example.aftersale.policy.rag.ingestion.domain;

import java.time.Instant;
import java.util.Objects;

public record PolicyIngestionChunk(
        String ingestionChunkId,
        String runId,
        String ingestionDocumentId,
        int chunkIndex,
        String content,
        int tokenEstimate,
        String checksum,
        String metadataJson,
        PolicyIngestionChunkStatus status,
        String errorMessage,
        Instant createdAt) {

    public PolicyIngestionChunk {
        ingestionChunkId = PolicyIngestionText.requireText(ingestionChunkId, "ingestionChunkId");
        runId = PolicyIngestionText.requireText(runId, "runId");
        ingestionDocumentId = PolicyIngestionText.requireText(ingestionDocumentId, "ingestionDocumentId");
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("chunkIndex must be greater than or equal to zero");
        }
        content = PolicyIngestionText.requireText(content, "content");
        if (tokenEstimate <= 0) {
            throw new IllegalArgumentException("tokenEstimate must be positive");
        }
        checksum = PolicyIngestionText.optionalText(checksum, "checksum");
        metadataJson = PolicyIngestionText.metadata(metadataJson);
        status = Objects.requireNonNull(status, "status must not be null");
        errorMessage = PolicyIngestionText.sanitizedOptionalText(errorMessage, "errorMessage");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
