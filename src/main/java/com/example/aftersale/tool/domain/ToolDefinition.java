package com.example.aftersale.tool.domain;

import java.util.Objects;

public record ToolDefinition(
        String toolName,
        String description,
        String inputSchema,
        String outputSchema,
        ToolRiskLevel riskLevel,
        boolean requiresApproval) {

    public ToolDefinition {
        toolName = requireText(toolName, "toolName");
        description = requireText(description, "description");
        inputSchema = requireText(inputSchema, "inputSchema");
        outputSchema = requireText(outputSchema, "outputSchema");
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        if (requiresApproval != riskLevel.requiresApproval()) {
            throw new IllegalArgumentException("requiresApproval must match riskLevel policy");
        }
    }

    public static ToolDefinition of(
            String toolName,
            String description,
            String inputSchema,
            String outputSchema,
            ToolRiskLevel riskLevel) {
        return new ToolDefinition(
                toolName,
                description,
                inputSchema,
                outputSchema,
                riskLevel,
                riskLevel.requiresApproval());
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
