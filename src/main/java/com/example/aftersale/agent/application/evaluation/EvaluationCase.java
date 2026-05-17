package com.example.aftersale.agent.application.evaluation;

import java.util.Objects;

public record EvaluationCase(
        String caseId,
        String userId,
        String orderId,
        String input,
        EvaluationExpected expected,
        String notes) {

    public EvaluationCase {
        caseId = requireText(caseId, "caseId");
        userId = requireText(userId, "userId");
        orderId = requireText(orderId, "orderId");
        input = requireText(input, "input");
        expected = Objects.requireNonNull(expected, "expected must not be null");
        notes = notes == null ? "" : notes;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
