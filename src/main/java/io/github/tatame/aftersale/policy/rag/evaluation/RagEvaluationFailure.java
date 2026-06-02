package io.github.tatame.aftersale.policy.rag.evaluation;

import java.util.Objects;

public record RagEvaluationFailure(
        String caseId,
        String field,
        String expected,
        String actual,
        String message) {

    public RagEvaluationFailure {
        caseId = requireText(caseId, "caseId");
        field = requireText(field, "field");
        expected = sanitize(requireText(expected, "expected"));
        actual = sanitize(requireText(actual, "actual"));
        message = sanitize(requireText(message, "message"));
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String sanitize(String value) {
        String sanitized = value
                .replaceAll("sk-[A-Za-z0-9_-]+", "sk-***")
                .replaceAll("Bearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer ***")
                .replaceAll("(?i)api[_-]?key\\s*[:=]\\s*[^\\s,;]+", "apiKey=***")
                .replaceAll("(?i)password\\s*[:=]\\s*[^\\s,;]+", "password=***")
                .replaceAll("(?i)token\\s*[:=]\\s*[^\\s,;]+", "token=***")
                .replaceAll("[A-Za-z]:\\\\[^\\s,;]+", "[local-path]");
        if (sanitized.length() > 240) {
            return sanitized.substring(0, 240);
        }
        return sanitized;
    }
}
