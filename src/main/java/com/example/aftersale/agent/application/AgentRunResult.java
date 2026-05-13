package com.example.aftersale.agent.application;

import com.example.aftersale.agent.domain.AgentRun;
import com.example.aftersale.ticket.domain.IntentType;
import java.util.List;
import java.util.Objects;

public record AgentRunResult(
        AgentRun agentRun,
        IntentType intent,
        String plan,
        String finalSuggestion,
        List<String> evidence,
        List<String> toolCalls) {

    public AgentRunResult {
        agentRun = Objects.requireNonNull(agentRun, "agentRun must not be null");
        intent = Objects.requireNonNull(intent, "intent must not be null");
        plan = requireText(plan, "plan");
        finalSuggestion = requireText(finalSuggestion, "finalSuggestion");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence must not be null"));
        toolCalls = List.copyOf(Objects.requireNonNull(toolCalls, "toolCalls must not be null"));
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
