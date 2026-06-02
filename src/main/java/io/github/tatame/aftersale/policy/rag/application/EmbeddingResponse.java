package io.github.tatame.aftersale.policy.rag.application;

import java.util.List;
import java.util.Objects;

public record EmbeddingResponse(
        String model,
        int dimension,
        List<Double> vector,
        Integer tokenEstimate) {

    public EmbeddingResponse {
        model = Objects.requireNonNull(model, "model must not be null");
        vector = List.copyOf(Objects.requireNonNull(vector, "vector must not be null"));
        if (model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        if (dimension <= 0) {
            throw new IllegalArgumentException("dimension must be positive");
        }
        if (vector.size() != dimension) {
            throw new IllegalArgumentException("dimension must match vector size");
        }
    }
}
