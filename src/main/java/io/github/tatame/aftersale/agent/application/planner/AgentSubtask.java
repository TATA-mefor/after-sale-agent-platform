package io.github.tatame.aftersale.agent.application.planner;

import io.github.tatame.aftersale.tool.domain.ToolRiskLevel;
import java.util.List;
import java.util.Objects;

public record AgentSubtask(
        String subtaskId,
        SubtaskType type,
        String target,
        String userMessageFragment,
        int priority,
        ToolRiskLevel riskLevel,
        String policyQuery,
        List<PlannedToolCall> plannedTools,
        List<String> dependencies,
        SubtaskStatus status) {

    public AgentSubtask {
        subtaskId = requirePresent(subtaskId, "subtaskId");
        type = Objects.requireNonNull(type, "type must not be null");
        target = requireText(target, "target");
        userMessageFragment = requireText(userMessageFragment, "userMessageFragment");
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        policyQuery = requirePresent(policyQuery, "policyQuery");
        plannedTools = List.copyOf(Objects.requireNonNull(plannedTools, "plannedTools must not be null"));
        if (plannedTools.isEmpty()) {
            throw new IllegalArgumentException("plannedTools must not be empty");
        }
        dependencies = List.copyOf(Objects.requireNonNull(dependencies, "dependencies must not be null"));
        status = Objects.requireNonNull(status, "status must not be null");
    }

    public AgentSubtask(
            String subtaskId,
            SubtaskType type,
            String target,
            String userMessageFragment,
            int priority,
            ToolRiskLevel riskLevel,
            String policyQuery,
            List<PlannedToolCall> plannedTools,
            List<String> dependencies) {
        this(
                subtaskId,
                type,
                target,
                userMessageFragment,
                priority,
                riskLevel,
                policyQuery,
                plannedTools,
                dependencies,
                SubtaskStatus.PENDING);
    }

    public AgentSubtask withStatus(SubtaskStatus newStatus) {
        return new AgentSubtask(
                subtaskId,
                type,
                target,
                userMessageFragment,
                priority,
                riskLevel,
                policyQuery,
                plannedTools,
                dependencies,
                newStatus);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String requirePresent(String value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " must not be null");
    }
}
