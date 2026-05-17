package com.example.aftersale.agent.application.executiontree;

import java.util.List;
import java.util.Objects;

public record ExecutionTreeSubtaskNode(
        String subtaskId,
        String type,
        String target,
        int priority,
        String riskLevel,
        String status,
        String summary,
        List<ExecutionTreeToolCallNode> toolCalls,
        List<ExecutionTreeApprovalNode> approvalRequests) {

    public ExecutionTreeSubtaskNode {
        subtaskId = requireText(subtaskId, "subtaskId");
        type = requireText(type, "type");
        target = target == null ? "" : target;
        riskLevel = riskLevel == null ? "" : riskLevel;
        status = status == null ? "" : status;
        summary = summary == null ? "" : summary;
        toolCalls = List.copyOf(Objects.requireNonNull(toolCalls, "toolCalls must not be null"));
        approvalRequests = List.copyOf(Objects.requireNonNull(
                approvalRequests,
                "approvalRequests must not be null"));
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
