package io.github.tatame.aftersale.policy.rag.evaluation;

import java.util.Objects;

public record RagEvaluationMetric(
        String name,
        double value,
        int passed,
        int total) {

    public RagEvaluationMetric {
        name = requireText(name, "name");
        if (value < 0.0d || value > 1.0d || !Double.isFinite(value)) {
            throw new IllegalArgumentException("value must be between 0.0 and 1.0");
        }
        if (passed < 0 || total < 0 || passed > total) {
            throw new IllegalArgumentException("metric counts must be valid");
        }
    }

    public static RagEvaluationMetric of(String name, int passed, int total) {
        double value = total == 0 ? 1.0d : (double) passed / total;
        return new RagEvaluationMetric(name, value, passed, total);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
