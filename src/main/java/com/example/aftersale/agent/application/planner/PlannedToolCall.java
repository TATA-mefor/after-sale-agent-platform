package com.example.aftersale.agent.application.planner;

import java.util.Objects;

public record PlannedToolCall(String toolName, String reason) {

    public PlannedToolCall {
        toolName = requireText(toolName, "toolName");
        reason = requireText(reason, "reason");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
