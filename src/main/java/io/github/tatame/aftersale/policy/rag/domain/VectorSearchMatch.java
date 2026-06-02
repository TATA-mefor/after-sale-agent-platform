package io.github.tatame.aftersale.policy.rag.domain;

import java.util.Objects;

public record VectorSearchMatch(
        String documentId,
        String chunkId,
        String documentTitle,
        String category,
        String productType,
        String snippet,
        double score,
        Double distance,
        String embeddingModel,
        String metadataJson) {

    public VectorSearchMatch {
        documentId = requireText(documentId, "documentId");
        chunkId = requireText(chunkId, "chunkId");
        documentTitle = requireText(documentTitle, "documentTitle");
        category = requireText(category, "category");
        productType = requireText(productType, "productType");
        snippet = requireText(snippet, "snippet");
        if (score < 0.0d || score > 1.0d || !Double.isFinite(score)) {
            throw new IllegalArgumentException("score must be between 0.0 and 1.0");
        }
        if (distance != null && (distance < 0.0d || !Double.isFinite(distance))) {
            throw new IllegalArgumentException("distance must be a non-negative finite number when provided");
        }
        embeddingModel = requireText(embeddingModel, "embeddingModel");
        metadataJson = normalizeMetadata(metadataJson);
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
