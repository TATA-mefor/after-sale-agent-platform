package com.example.aftersale.agent.infrastructure.llm;

import com.example.aftersale.agent.application.planner.AgentPlan;
import com.example.aftersale.agent.application.planner.AgentPlanValidationException;
import com.example.aftersale.agent.application.planner.AgentSubtask;
import com.example.aftersale.agent.application.planner.PlannedToolCall;
import com.example.aftersale.agent.application.planner.SubtaskType;
import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;

/**
 * 将 Provider 文本解析为 Java 后端使用的严格 AgentPlan 结构。
 *
 * <p>边界：解析只接受 AgentPlan 所需 JSON 字段；它不执行工具、不修复不安全的 Provider 输出，
 * 也不把未知子任务类型当作可执行工作。
 */
public class AgentPlanParser {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "ObjectMapper is an application-wide JSON collaborator injected by Spring.")
    public AgentPlanParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将 Provider 原始输出转换为 AgentPlan；失败时在编排开始前抛出异常。
     */
    public AgentPlan parse(String rawContent) {
        JsonNode root = parseJson(rawContent);
        return new AgentPlan(
                parseIntent(root),
                parseRiskLevel(root),
                requireText(root, "policyQuery"),
                requireText(root, "noteToAdd"),
                requireText(root, "finalSuggestion"),
                parseStringArray(root, "evidenceHints"),
                parsePlannedTools(root, "plannedTools"),
                parseSubtasks(root));
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

    private static List<AgentSubtask> parseSubtasks(JsonNode root) {
        JsonNode value = root.get("subtasks");
        if (value == null) {
            return List.of();
        }
        if (!value.isArray()) {
            throw new AgentPlanValidationException("subtasks must be an array");
        }
        List<AgentSubtask> result = new ArrayList<>();
        for (JsonNode item : value) {
            if (!item.isObject()) {
                throw new AgentPlanValidationException("subtasks entries must be objects");
            }
            result.add(new AgentSubtask(
                    requireText(item, "subtaskId"),
                    parseSubtaskType(item),
                    requireText(item, "target"),
                    requireText(item, "userMessageFragment"),
                    parsePriority(item),
                    parseRiskLevel(item),
                    requireText(item, "policyQuery"),
                    parsePlannedTools(item, "plannedTools"),
                    parseStringArray(item, "dependencies")));
        }
        return result;
    }

    private static SubtaskType parseSubtaskType(JsonNode root) {
        String value = requireText(root, "type");
        try {
            return SubtaskType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            // 未知类型保留下来交给 AgentPlanValidator，以策略级错误拒绝。
            return SubtaskType.UNKNOWN;
        }
    }

    private static int parsePriority(JsonNode root) {
        JsonNode value = root.get("priority");
        if (value == null || !value.canConvertToInt()) {
            throw new AgentPlanValidationException("priority must be an integer");
        }
        return value.asInt();
    }

    private static List<PlannedToolCall> parsePlannedTools(JsonNode root, String fieldName) {
        JsonNode value = root.get(fieldName);
        if (value == null || !value.isArray() || value.isEmpty()) {
            throw new AgentPlanValidationException(fieldName + " must be a non-empty array");
        }
        List<PlannedToolCall> result = new ArrayList<>();
        for (JsonNode item : value) {
            if (!item.isObject()) {
                throw new AgentPlanValidationException(fieldName + " entries must be objects");
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
