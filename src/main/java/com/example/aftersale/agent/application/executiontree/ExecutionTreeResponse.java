package com.example.aftersale.agent.application.executiontree;

import com.example.aftersale.agent.domain.AgentRunStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ExecutionTreeResponse(
        String runId,
        String ticketId,
        AgentRunStatus agentRunStatus,
        String finalSuggestion,
        String rootSummary,
        List<ExecutionTreeSubtaskNode> subtasks,
        List<ExecutionTreeToolCallNode> toolCalls,
        List<ExecutionTreePolicyEvidenceNode> policyEvidence,
        List<ExecutionTreeApprovalNode> approvalRequests,
        List<String> errors,
        Instant createdAt,
        Instant finishedAt) {

    public ExecutionTreeResponse {
        runId = requireText(runId, "runId");
        ticketId = requireText(ticketId, "ticketId");
        agentRunStatus = Objects.requireNonNull(agentRunStatus, "agentRunStatus must not be null");
        finalSuggestion = finalSuggestion == null ? "" : finalSuggestion;
        rootSummary = rootSummary == null ? "" : rootSummary;
        subtasks = List.copyOf(Objects.requireNonNull(subtasks, "subtasks must not be null"));
        toolCalls = List.copyOf(Objects.requireNonNull(toolCalls, "toolCalls must not be null"));
        policyEvidence = List.copyOf(Objects.requireNonNull(
                policyEvidence,
                "policyEvidence must not be null"));
        approvalRequests = List.copyOf(Objects.requireNonNull(
                approvalRequests,
                "approvalRequests must not be null"));
        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
