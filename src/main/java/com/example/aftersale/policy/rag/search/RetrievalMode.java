package com.example.aftersale.policy.rag.search;

import java.util.Locale;

public enum RetrievalMode {
    KEYWORD,
    VECTOR,
    HYBRID;

    public static RetrievalMode defaultMode() {
        return KEYWORD;
    }

    public static RetrievalMode parse(String value) {
        if (value == null || value.isBlank()) {
            return defaultMode();
        }
        try {
            return RetrievalMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown retrievalMode: " + value, ex);
        }
    }
}
