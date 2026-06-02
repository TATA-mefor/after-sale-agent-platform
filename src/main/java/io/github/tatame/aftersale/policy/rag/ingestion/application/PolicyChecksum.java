package io.github.tatame.aftersale.policy.rag.ingestion.application;

import java.util.Objects;

public record PolicyChecksum(ChecksumAlgorithm algorithm, String value) {

    public PolicyChecksum {
        algorithm = Objects.requireNonNull(algorithm, "algorithm must not be null");
        value = requireText(value, "value");
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
