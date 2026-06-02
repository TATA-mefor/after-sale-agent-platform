package io.github.tatame.aftersale.agent.application.executiontree;

import java.util.Objects;
import org.springframework.lang.Nullable;

public record ExecutionTreePolicyEvidenceNode(
        String evidenceId,
        String policyId,
        String documentId,
        String chunkId,
        String documentTitle,
        String category,
        String productType,
        String snippet,
        @Nullable
        Double score,
        String retrievalMode,
        String source,
        String subtaskId,
        String toolCallId) {

    public ExecutionTreePolicyEvidenceNode {
        evidenceId = optional(evidenceId);
        policyId = optional(policyId);
        documentId = optional(documentId);
        chunkId = optional(chunkId);
        documentTitle = optional(documentTitle);
        category = requireText(sanitize(category), "category");
        productType = optional(productType);
        snippet = requireText(sanitize(snippet), "snippet");
        if (score != null && (score < 0.0d || score > 1.0d)) {
            throw new IllegalArgumentException("score must be between 0.0 and 1.0");
        }
        retrievalMode = optional(retrievalMode);
        source = optional(source);
        subtaskId = optional(subtaskId);
        toolCallId = optional(toolCallId);
    }

    private static String optional(String value) {
        if (value == null) {
            return "";
        }
        return sanitize(value);
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value
                .replaceAll("sk-[A-Za-z0-9_-]{8,}", "sk-***")
                .replaceAll("Bearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer ***")
                .replaceAll("(?i)api[_-]?key\\s*[:=]\\s*[^\\s,;]+", "apiKey=***")
                .replaceAll("(?i)password\\s*[:=]\\s*[^\\s,;]+", "password=***")
                .replaceAll("(?i)token\\s*[:=]\\s*[^\\s,;]+", "token=***")
                .replaceAll("[A-Za-z]:\\\\[^\\s,;]+", "[local-path]")
                .replaceAll("\\s+", " ")
                .trim();
        if (sanitized.length() > 180) {
            return sanitized.substring(0, 177) + "...";
        }
        return sanitized;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
