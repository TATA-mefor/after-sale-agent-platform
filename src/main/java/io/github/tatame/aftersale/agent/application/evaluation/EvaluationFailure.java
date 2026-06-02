package io.github.tatame.aftersale.agent.application.evaluation;

import java.util.Objects;

public record EvaluationFailure(
        String caseId,
        String field,
        String expected,
        String actual,
        String message) {

    public EvaluationFailure {
        caseId = requireText(caseId, "caseId");
        field = requireText(field, "field");
        expected = expected == null ? "" : expected;
        actual = actual == null ? "" : actual;
        message = requireText(message, "message");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
