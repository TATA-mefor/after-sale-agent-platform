package com.example.aftersale.policy.rag.search;

import java.util.List;
import java.util.Objects;

public record RagPolicySearchResult(
        String query,
        RetrievalMode retrievalMode,
        List<RagPolicyEvidence> evidences,
        String message,
        boolean fallbackUsed,
        int totalKeywordMatches,
        int totalVectorMatches) {

    public RagPolicySearchResult {
        query = requireText(query, "query");
        retrievalMode = Objects.requireNonNull(retrievalMode, "retrievalMode must not be null");
        evidences = List.copyOf(Objects.requireNonNull(evidences, "evidences must not be null"));
        message = requireText(message, "message");
        if (totalKeywordMatches < 0) {
            throw new IllegalArgumentException("totalKeywordMatches must not be negative");
        }
        if (totalVectorMatches < 0) {
            throw new IllegalArgumentException("totalVectorMatches must not be negative");
        }
    }

    public boolean hasEvidence() {
        return !evidences.isEmpty();
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
