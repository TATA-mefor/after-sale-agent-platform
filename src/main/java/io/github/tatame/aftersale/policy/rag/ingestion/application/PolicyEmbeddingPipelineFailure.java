package io.github.tatame.aftersale.policy.rag.ingestion.application;

import java.util.Objects;

public record PolicyEmbeddingPipelineFailure(
        String ingestionChunkId,
        String errorCode,
        String message,
        String sanitizedDetails) {

    public PolicyEmbeddingPipelineFailure {
        ingestionChunkId = optionalText(ingestionChunkId, "ingestionChunkId");
        errorCode = requireText(errorCode, "errorCode");
        message = sanitize(requireText(message, "message"));
        sanitizedDetails = sanitizedDetails == null ? null : sanitize(sanitizedDetails);
    }

    static String sanitize(String value) {
        String sanitized = Objects.requireNonNull(value, "value must not be null")
                .replaceAll("(?i)(api[_-]?key|password|token)\\s*[=:]\\s*[^\\s,;]+", "$1=<redacted>")
                .replaceAll("(?i)prompt\\s*[=:]\\s*[^\\n\\r]{0,200}", "prompt=<redacted>")
                .replaceAll("[A-Za-z]:\\\\[^\\s,;]+", "<local-path-redacted>")
                .replaceAll("/(?:Users|home|var|tmp)/[^\\s,;]+", "<local-path-redacted>");
        if (sanitized.length() > 240) {
            return sanitized.substring(0, 240).trim();
        }
        return sanitized.trim();
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
