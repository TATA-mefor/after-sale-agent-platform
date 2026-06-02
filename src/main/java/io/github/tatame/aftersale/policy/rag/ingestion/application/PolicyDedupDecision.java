package io.github.tatame.aftersale.policy.rag.ingestion.application;

import java.util.Objects;

public record PolicyDedupDecision(
        PolicyDedupDecisionType type,
        String reason,
        String existingDocumentId,
        String existingChunkId) {

    public PolicyDedupDecision {
        type = Objects.requireNonNull(type, "type must not be null");
        reason = requireText(reason, "reason");
        existingDocumentId = optionalText(existingDocumentId, "existingDocumentId");
        existingChunkId = optionalText(existingChunkId, "existingChunkId");
    }

    public boolean duplicate() {
        return type != PolicyDedupDecisionType.NEW_CONTENT;
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
