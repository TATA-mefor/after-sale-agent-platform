package com.example.aftersale.policy.rag.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PolicyEmbedding(
        String embeddingId,
        String chunkId,
        String embeddingModel,
        int embeddingDimension,
        List<Double> vector,
        Instant createdAt) {

    public PolicyEmbedding {
        embeddingId = requireText(embeddingId, "embeddingId");
        chunkId = requireText(chunkId, "chunkId");
        embeddingModel = requireText(embeddingModel, "embeddingModel");
        if (embeddingDimension <= 0) {
            throw new IllegalArgumentException("embeddingDimension must be positive");
        }
        vector = List.copyOf(Objects.requireNonNull(vector, "vector must not be null"));
        if (vector.isEmpty()) {
            throw new IllegalArgumentException("vector must not be empty");
        }
        if (vector.size() != embeddingDimension) {
            throw new IllegalArgumentException("embeddingDimension must match vector size");
        }
        if (vector.stream().anyMatch(value -> value == null || !Double.isFinite(value))) {
            throw new IllegalArgumentException("vector values must be finite numbers");
        }
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
}
