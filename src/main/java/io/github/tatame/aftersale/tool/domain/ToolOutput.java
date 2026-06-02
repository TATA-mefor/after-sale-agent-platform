package io.github.tatame.aftersale.tool.domain;

import java.util.Map;
import java.util.Objects;

public record ToolOutput(
        String toolName,
        ToolExecutionStatus status,
        Map<String, Object> data,
        String errorCode,
        String message) {

    public ToolOutput {
        toolName = requireText(toolName, "toolName");
        status = Objects.requireNonNull(status, "status must not be null");
        data = data == null ? Map.of() : Map.copyOf(data);
        message = requireText(message, "message");
        if (status == ToolExecutionStatus.FAILED) {
            errorCode = requireText(errorCode, "errorCode");
        }
    }

    public static ToolOutput succeeded(String toolName, Map<String, Object> data) {
        return new ToolOutput(toolName, ToolExecutionStatus.SUCCEEDED, data, null, "ok");
    }

    public static ToolOutput failure(String toolName, String errorCode, String errorMessage) {
        return new ToolOutput(toolName, ToolExecutionStatus.FAILED, Map.of(), errorCode, errorMessage);
    }

    public static ToolOutput requiresApproval(String toolName, String approvalMessage) {
        return new ToolOutput(
                toolName,
                ToolExecutionStatus.REQUIRES_APPROVAL,
                Map.of("requiresApproval", true),
                null,
                approvalMessage);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
