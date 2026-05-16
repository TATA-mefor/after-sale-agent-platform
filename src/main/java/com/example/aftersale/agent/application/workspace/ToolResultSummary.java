package com.example.aftersale.agent.application.workspace;

import com.example.aftersale.tool.domain.ToolExecutionStatus;
import com.example.aftersale.tool.domain.ToolOutput;
import java.time.Instant;
import java.util.Objects;

public record ToolResultSummary(
        String subtaskId,
        String toolName,
        ToolExecutionStatus status,
        String summary,
        Instant createdAt) {

    public ToolResultSummary {
        subtaskId = subtaskId == null ? "" : subtaskId;
        toolName = requireText(toolName, "toolName");
        status = Objects.requireNonNull(status, "status must not be null");
        summary = requireText(summary, "summary");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static ToolResultSummary fromToolOutput(String subtaskId, ToolOutput output, Instant createdAt) {
        return new ToolResultSummary(
                subtaskId,
                output.toolName(),
                output.status(),
                output.message(),
                createdAt);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
