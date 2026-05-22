package com.example.aftersale.policy.rag.domain;

import java.util.List;
import java.util.Objects;

/**
 * Computes deterministic retrieval evidence scores; the score is not a business decision confidence.
 */
public final class CosineSimilarityCalculator {

    public double similarity(List<Double> left, List<Double> right) {
        List<Double> normalizedLeft = validate(left, "left");
        List<Double> normalizedRight = validate(right, "right");
        if (normalizedLeft.size() != normalizedRight.size()) {
            throw new IllegalArgumentException("vector dimensions must match");
        }

        double dotProduct = 0.0d;
        double leftMagnitude = 0.0d;
        double rightMagnitude = 0.0d;
        for (int index = 0; index < normalizedLeft.size(); index++) {
            double leftValue = normalizedLeft.get(index);
            double rightValue = normalizedRight.get(index);
            dotProduct += leftValue * rightValue;
            leftMagnitude += leftValue * leftValue;
            rightMagnitude += rightValue * rightValue;
        }

        if (leftMagnitude == 0.0d || rightMagnitude == 0.0d) {
            return 0.0d;
        }
        double score = dotProduct / (Math.sqrt(leftMagnitude) * Math.sqrt(rightMagnitude));
        return Math.max(0.0d, Math.min(1.0d, score));
    }

    private static List<Double> validate(List<Double> vector, String fieldName) {
        List<Double> normalized = List.copyOf(Objects.requireNonNull(vector, fieldName + " vector must not be null"));
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " vector must not be empty");
        }
        if (normalized.stream().anyMatch(value -> value == null || !Double.isFinite(value))) {
            throw new IllegalArgumentException(fieldName + " vector values must be finite numbers");
        }
        return normalized;
    }
}
