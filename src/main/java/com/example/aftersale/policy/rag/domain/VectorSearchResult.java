package com.example.aftersale.policy.rag.domain;

import java.util.List;
import java.util.Objects;

public record VectorSearchResult(
        List<VectorSearchMatch> matches,
        String message,
        boolean fallbackUsed) {

    public VectorSearchResult {
        matches = List.copyOf(Objects.requireNonNull(matches, "matches must not be null"));
        message = requireText(message, "message");
    }

    public static VectorSearchResult matched(List<VectorSearchMatch> matches) {
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("matches must not be empty for a matched vector search result");
        }
        return new VectorSearchResult(matches, "Found policy evidence matches.", false);
    }

    public static VectorSearchResult empty(String message, boolean fallbackUsed) {
        return new VectorSearchResult(List.of(), message, fallbackUsed);
    }

    public boolean hasMatches() {
        return !matches.isEmpty();
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
