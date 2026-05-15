package com.example.aftersale.policy.domain;

import java.util.List;
import java.util.Objects;

public record PolicySearchResult(
        PolicySearchQuery query,
        List<PolicySnippet> snippets,
        String message) {

    public PolicySearchResult {
        query = Objects.requireNonNull(query, "query must not be null");
        snippets = List.copyOf(Objects.requireNonNull(snippets, "snippets must not be null"));
        message = requireText(message, "message");
    }

    public static PolicySearchResult matched(PolicySearchQuery query, List<PolicySnippet> snippets) {
        if (snippets.isEmpty()) {
            throw new IllegalArgumentException("snippets must not be empty for a matched policy result");
        }
        return new PolicySearchResult(query, snippets, "Matched after-sale policies.");
    }

    public static PolicySearchResult empty(PolicySearchQuery query) {
        return new PolicySearchResult(query, List.of(), "No after-sale policy matched the query.");
    }

    public boolean hasMatches() {
        return !snippets.isEmpty();
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
