package com.example.aftersale.agent.application.planner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AgentPlanValidator {

    private AgentPlanValidator() {
    }

    public static void validate(AgentPlan plan, List<String> availableTools) {
        Set<String> availableToolNames = new HashSet<>(availableTools);
        for (PlannedToolCall plannedTool : plan.plannedTools()) {
            if (!availableToolNames.contains(plannedTool.toolName())) {
                throw new IllegalArgumentException("Planner returned unknown tool: " + plannedTool.toolName());
            }
        }
    }
}
