package com.example.aftersale.agent.prompt;

import com.example.aftersale.agent.application.planner.AgentPlanningContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;

public class AgentPlannerPromptFactory {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "ObjectMapper is an application-wide JSON collaborator injected by Spring.")
    public AgentPlannerPromptFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String systemPrompt() {
        return """
                You are the planner for an after-sale ticket Agent.
                Return only one JSON object matching the AgentPlan schema.
                You may plan tools, but you must not execute tools.
                You must not claim refunds, compensation, dispute closure, payment changes, or other high-risk actions
                have already been completed.
                Java backend validates the plan and executes tools through ToolRegistry.
                """;
    }

    public String userPrompt(AgentPlanningContext context) {
        return "Create an AgentPlan for this context:\n" + toJson(Map.of(
                "ticketId", context.ticketId(),
                "userId", context.userId(),
                "orderId", context.orderId(),
                "rawUserMessage", context.rawUserMessage(),
                "currentTicketStatus", context.currentTicketStatus().name(),
                "availableTools", context.availableTools(),
                "riskPolicySummary", context.riskPolicySummary(),
                "createdAt", context.createdAt().toString(),
                "requiredJsonFields", Map.of(
                        "intent", "supported IntentType enum",
                        "riskLevel", "LOW | MEDIUM | HIGH",
                        "policyQuery", "non-empty policy search query",
                        "noteToAdd", "safe internal note",
                        "finalSuggestion", "safe final suggestion",
                        "evidenceHints", "array of strings",
                        "plannedTools", "array of {toolName, reason}")));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize LLM planning prompt", exception);
        }
    }
}
