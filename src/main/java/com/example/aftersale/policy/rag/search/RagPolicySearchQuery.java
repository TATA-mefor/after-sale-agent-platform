package com.example.aftersale.policy.rag.search;

import java.time.LocalDate;
import java.util.Objects;

public record RagPolicySearchQuery(
        String query,
        RetrievalMode retrievalMode,
        int topK,
        Double minScore,
        String category,
        String productType,
        LocalDate effectiveAt,
        String embeddingModel,
        boolean includeKeywordEvidence,
        boolean includeVectorEvidence) {

    public static final int DEFAULT_TOP_K = 5;
    public static final int MAX_TOP_K = 20;

    public RagPolicySearchQuery {
        query = requireText(query, "query");
        retrievalMode = retrievalMode == null ? RetrievalMode.defaultMode() : retrievalMode;
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

    public static RagPolicySearchQuery keyword(String query) {
        return new RagPolicySearchQuery(
                query,
                RetrievalMode.KEYWORD,
                DEFAULT_TOP_K,
                null,
                null,
                null,
                null,
                null,
                true,
                false);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
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
