package com.example.aftersale.agent.application.planner;

import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import java.util.List;
import java.util.Objects;

public record AgentPlan(
        IntentType intent,
        ToolRiskLevel riskLevel,
        String policyQuery,
        String noteToAdd,
        String finalSuggestion,
        List<String> evidenceHints,
        List<PlannedToolCall> plannedTools) {

    public AgentPlan {
        intent = Objects.requireNonNull(intent, "intent must not be null");
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        policyQuery = requireText(policyQuery, "policyQuery");
        noteToAdd = requireText(noteToAdd, "noteToAdd");
        finalSuggestion = requireText(finalSuggestion, "finalSuggestion");
        evidenceHints = List.copyOf(Objects.requireNonNull(evidenceHints, "evidenceHints must not be null"));
        plannedTools = List.copyOf(Objects.requireNonNull(plannedTools, "plannedTools must not be null"));
        if (plannedTools.isEmpty()) {
            throw new IllegalArgumentException("plannedTools must not be empty");
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
