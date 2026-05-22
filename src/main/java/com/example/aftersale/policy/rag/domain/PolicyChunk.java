package com.example.aftersale.policy.rag.domain;

import java.time.Instant;
import java.util.Objects;

public record PolicyChunk(
        String chunkId,
        String documentId,
        int chunkIndex,
        String content,
        int tokenEstimate,
        String metadataJson,
        Instant createdAt) {

    public PolicyChunk {
        chunkId = requireText(chunkId, "chunkId");
        documentId = requireText(documentId, "documentId");
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("chunkIndex must be greater than or equal to zero");
        }
        content = requireText(content, "content");
        if (tokenEstimate < 0) {
            throw new IllegalArgumentException("tokenEstimate must be greater than or equal to zero");
        }
        metadataJson = normalizeMetadata(metadataJson);
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeMetadata(String value) {
        if (value == null || value.isBlank()) {
            return "{}";
        }
        return value.trim();
    }
}
