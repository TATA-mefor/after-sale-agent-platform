package io.github.tatame.aftersale.policy.rag.evaluation;

import io.github.tatame.aftersale.policy.rag.search.RetrievalMode;
import java.util.Objects;

public record RagEvaluationCase(
        String caseId,
        String query,
        RetrievalMode retrievalMode,
        int topK,
        Double minScore,
        String category,
        String productType,
        RagEvaluationExpected expected) {

    public RagEvaluationCase {
        caseId = requireText(caseId, "caseId");
        query = requireText(query, "query");
        retrievalMode = retrievalMode == null ? RetrievalMode.defaultMode() : retrievalMode;
        if (topK < 1 || topK > 20) {
            throw new IllegalArgumentException("topK must be between 1 and 20");
        }
        if (minScore != null && (minScore < 0.0d || minScore > 1.0d || !Double.isFinite(minScore))) {
            throw new IllegalArgumentException("minScore must be between 0.0 and 1.0 when provided");
        }
        category = optionalText(category, "category");
        productType = optionalText(productType, "productType");
        expected = Objects.requireNonNull(expected, "expected must not be null");
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
