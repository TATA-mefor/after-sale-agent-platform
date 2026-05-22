package com.example.aftersale.policy.rag.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record VectorSearchQuery(
        String queryText,
        List<Double> queryVector,
        int topK,
        Double minScore,
        String category,
        String productType,
        LocalDate effectiveAt,
        String embeddingModel) {

    public static final int MAX_TOP_K = 50;

    public VectorSearchQuery {
        queryText = optionalText(queryText, "queryText");
        queryVector = List.copyOf(Objects.requireNonNull(queryVector, "queryVector must not be null"));
        if (queryVector.isEmpty()) {
            throw new IllegalArgumentException("queryVector must not be empty");
        }
        if (queryVector.stream().anyMatch(value -> value == null || !Double.isFinite(value))) {
            throw new IllegalArgumentException("queryVector values must be finite numbers");
        }
        if (topK < 1 || topK > MAX_TOP_K) {
            throw new IllegalArgumentException("topK must be between 1 and " + MAX_TOP_K);
        }
        if (minScore != null && (minScore < 0.0d || minScore > 1.0d || !Double.isFinite(minScore))) {
            throw new IllegalArgumentException("minScore must be between 0.0 and 1.0 when provided");
        }
        category = optionalText(category, "category");
        productType = optionalText(productType, "productType");
        embeddingModel = optionalText(embeddingModel, "embeddingModel");
    }

    private static String optionalText(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank when provided");
        }
        return normalized;
    }
}
