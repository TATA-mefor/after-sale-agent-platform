package io.github.tatame.aftersale.tool.domain;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record ToolInput(Map<String, Object> arguments) {

    public ToolInput {
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }

    public static ToolInput empty() {
        return new ToolInput(Map.of());
    }

    public static ToolInput of(Map<String, Object> arguments) {
        return new ToolInput(arguments);
    }

    public String requireString(String fieldName) {
        Object value = arguments.get(requireFieldName(fieldName));
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be a non-blank string");
        }
        return text;
    }

    public Optional<String> optionalString(String fieldName) {
        Object value = arguments.get(requireFieldName(fieldName));
        if (value == null) {
            return Optional.empty();
        }
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be a non-blank string");
        }
        return Optional.of(text);
    }

    private static String requireFieldName(String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName must not be null");
        if (fieldName.isBlank()) {
            throw new IllegalArgumentException("fieldName must not be blank");
        }
        return fieldName;
    }
}
