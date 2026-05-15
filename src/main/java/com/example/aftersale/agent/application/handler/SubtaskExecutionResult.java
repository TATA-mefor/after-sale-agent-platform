package com.example.aftersale.agent.application.handler;

import com.example.aftersale.agent.application.planner.SubtaskStatus;
import com.example.aftersale.agent.application.planner.SubtaskType;
import java.util.List;
import java.util.Objects;

public record SubtaskExecutionResult(
        String subtaskId,
        SubtaskType type,
        SubtaskStatus status,
        String summary,
        List<String> evidence,
        List<String> toolCalls,
        String errorMessage,
        boolean requiresHumanApproval) {

    public SubtaskExecutionResult {
        subtaskId = requireText(subtaskId, "subtaskId");
        type = Objects.requireNonNull(type, "type must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        summary = requireText(summary, "summary");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence must not be null"));
        toolCalls = List.copyOf(Objects.requireNonNull(toolCalls, "toolCalls must not be null"));
        errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public static SubtaskExecutionResult succeeded(
            String subtaskId,
            SubtaskType type,
            String summary,
            List<String> evidence,
            List<String> toolCalls) {
        return new SubtaskExecutionResult(
                subtaskId,
                type,
                SubtaskStatus.SUCCEEDED,
                summary,
                evidence,
                toolCalls,
                "",
                false);
    }

    public static SubtaskExecutionResult failed(String subtaskId, SubtaskType type, String errorMessage) {
        return new SubtaskExecutionResult(
                subtaskId,
                type,
                SubtaskStatus.FAILED,
                "Subtask " + subtaskId + " " + type.name() + " failed: " + errorMessage,
                List.of(),
                List.of(),
                requireText(errorMessage, "errorMessage"),
                false);
    }

    public static SubtaskExecutionResult requiresApproval(String subtaskId, SubtaskType type, String summary) {
        return new SubtaskExecutionResult(
                subtaskId,
                type,
                SubtaskStatus.WAITING_APPROVAL,
                summary,
                List.of(),
                List.of(),
                "",
                true);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
