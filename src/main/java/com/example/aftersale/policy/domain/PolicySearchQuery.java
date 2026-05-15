package com.example.aftersale.policy.domain;

import java.util.Locale;
import java.util.Objects;

public record PolicySearchQuery(String queryText, int limit) {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;

    public PolicySearchQuery {
        queryText = normalize(queryText);
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
    }

    public static PolicySearchQuery of(String queryText) {
        return new PolicySearchQuery(queryText, DEFAULT_LIMIT);
    }

    private static String normalize(String value) {
        Objects.requireNonNull(value, "queryText must not be null");
        String normalized = value.toLowerCase(Locale.ROOT).trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("queryText must not be blank");
        }
        return normalized;
    }
}
