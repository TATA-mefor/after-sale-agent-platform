package com.example.aftersale.agent.infrastructure.llm;

import com.example.aftersale.agent.application.planner.AgentPlan;
import com.example.aftersale.agent.application.planner.AgentPlanValidationException;
import com.example.aftersale.agent.application.planner.PlannedToolCall;
import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;

public class AgentPlanParser {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "ObjectMapper is an application-wide JSON collaborator injected by Spring.")
    public AgentPlanParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AgentPlan parse(String rawContent) {
        JsonNode root = parseJson(rawContent);
        return new AgentPlan(
                parseIntent(root),
                parseRiskLevel(root),
                requireText(root, "policyQuery"),
                requireText(root, "noteToAdd"),
                requireText(root, "finalSuggestion"),
                parseStringArray(root, "evidenceHints"),
                parsePlannedTools(root));
    }

    private JsonNode parseJson(String rawContent) {
        try {
            return objectMapper.readTree(rawContent);
        } catch (JsonProcessingException exception) {
            throw new AgentPlanValidationException("LLM AgentPlan output is not valid JSON", exception);
        }
    }

    private static IntentType parseIntent(JsonNode root) {
        String value = requireText(root, "intent");
        try {
            return IntentType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new AgentPlanValidationException("Unsupported intent in LLM AgentPlan: " + value, exception);
        }
    }

    private static ToolRiskLevel parseRiskLevel(JsonNode root) {
        String value = requireText(root, "riskLevel");
        try {
            return ToolRiskLevel.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new AgentPlanValidationException("Unsupported riskLevel in LLM AgentPlan: " + value, exception);
        }
    }

    private static List<String> parseStringArray(JsonNode root, String fieldName) {
        JsonNode value = root.get(fieldName);
        if (value == null || !value.isArray()) {
            throw new AgentPlanValidationException(fieldName + " must be an array");
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : value) {
            if (!item.isTextual() || item.asText().isBlank()) {
                throw new AgentPlanValidationException(fieldName + " must contain only non-blank strings");
            }
            result.add(item.asText());
        }
        return result;
    }

    private static List<PlannedToolCall> parsePlannedTools(JsonNode root) {
        JsonNode value = root.get("plannedTools");
        if (value == null || !value.isArray() || value.isEmpty()) {
            throw new AgentPlanValidationException("plannedTools must be a non-empty array");
        }
        List<PlannedToolCall> result = new ArrayList<>();
        for (JsonNode item : value) {
            if (!item.isObject()) {
                throw new AgentPlanValidationException("plannedTools entries must be objects");
            }
            result.add(new PlannedToolCall(
                    requireText(item, "toolName"),
                    requireText(item, "reason")));
        }
        return result;
    }

    private static String requireText(JsonNode root, String fieldName) {
        JsonNode value = root.get(fieldName);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new AgentPlanValidationException(fieldName + " must be a non-blank string");
        }
        return value.asText();
    }
}
