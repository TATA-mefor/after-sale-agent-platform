package com.example.aftersale.agent.application.planner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AgentPlanValidator {

    private static final List<String> UNSAFE_COMPLETION_CLAIMS = List.of(
            "已退款",
            "已完成退款",
            "退款已完成",
            "已补偿",
            "已发放补偿",
            "已关闭争议",
            "争议已关闭",
            "refund completed",
            "refunded",
            "compensation issued",
            "dispute closed");

    private AgentPlanValidator() {
    }

    public static void validate(AgentPlan plan, List<String> availableTools) {
        Set<String> availableToolNames = new HashSet<>(availableTools);
        for (PlannedToolCall plannedTool : plan.plannedTools()) {
            if (!availableToolNames.contains(plannedTool.toolName())) {
                throw new AgentPlanValidationException("Planner returned unknown tool: " + plannedTool.toolName());
            }
        }
        ensureSafeText(plan.noteToAdd(), "noteToAdd");
        ensureSafeText(plan.finalSuggestion(), "finalSuggestion");
    }

    private static void ensureSafeText(String value, String fieldName) {
        String normalized = value.toLowerCase();
        for (String unsafeClaim : UNSAFE_COMPLETION_CLAIMS) {
            if (normalized.contains(unsafeClaim)) {
                throw new AgentPlanValidationException(
                        fieldName + " contains an unsafe high-risk completion claim: " + unsafeClaim);
            }
        }
    }
}
