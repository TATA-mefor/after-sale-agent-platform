package io.github.tatame.aftersale.policy.rag.application;

import java.util.Objects;

public record EmbeddingRequest(String model, String text) {

    public EmbeddingRequest {
        model = requireText(model, "model");
        text = requireText(text, "text");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
