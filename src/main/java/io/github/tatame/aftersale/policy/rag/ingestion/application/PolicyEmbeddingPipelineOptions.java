package io.github.tatame.aftersale.policy.rag.ingestion.application;

import java.util.Objects;

public record PolicyEmbeddingPipelineOptions(
        String embeddingModel,
        Integer expectedDimension,
        boolean failOnDimensionMismatch,
        boolean skipDuplicateEmbeddings,
        int maxChunksPerRun,
        int batchSize) {

    public static final String DEFAULT_MODEL = "fake-policy-embedding";

    public static PolicyEmbeddingPipelineOptions defaults() {
        return new PolicyEmbeddingPipelineOptions(DEFAULT_MODEL, null, true, true, 100, 16);
    }

    public PolicyEmbeddingPipelineOptions {
        embeddingModel = requireText(embeddingModel, "embeddingModel");
        if (expectedDimension != null && expectedDimension <= 0) {
            throw new IllegalArgumentException("expectedDimension must be positive when provided");
        }
        if (maxChunksPerRun <= 0) {
            throw new IllegalArgumentException("maxChunksPerRun must be positive");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
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
