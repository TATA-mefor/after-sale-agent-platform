package io.github.tatame.aftersale.agent.application.executiontree;

import io.github.tatame.aftersale.trace.domain.ToolCallStatus;
import java.time.Instant;
import java.util.Objects;

public record ExecutionTreeToolCallNode(
        String traceId,
        String toolName,
        ToolCallStatus status,
        long latencyMs,
        String inputJson,
        String outputJson,
        String errorMessage,
        Instant createdAt) {

    public ExecutionTreeToolCallNode {
        traceId = requireText(traceId, "traceId");
        toolName = requireText(toolName, "toolName");
        status = Objects.requireNonNull(status, "status must not be null");
        inputJson = requireText(inputJson, "inputJson");
        outputJson = outputJson == null ? "" : outputJson;
        errorMessage = errorMessage == null ? "" : errorMessage;
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (latencyMs < 0) {
            throw new IllegalArgumentException("latencyMs must not be negative");
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
