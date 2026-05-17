package com.example.aftersale.agent.application.evaluation;

import java.util.Objects;

public record EvaluationMetric(
        String name,
        int passed,
        int total,
        double value) {

    public EvaluationMetric {
        name = requireText(name, "name");
        if (passed < 0 || total < 0) {
            throw new IllegalArgumentException("metric counts must not be negative");
        }
        if (passed > total) {
            throw new IllegalArgumentException("passed must not exceed total");
        }
    }

    public static EvaluationMetric of(String name, int passed, int total) {
        double value = total == 0 ? 0.0D : (double) passed / total;
        return new EvaluationMetric(name, passed, total, value);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
