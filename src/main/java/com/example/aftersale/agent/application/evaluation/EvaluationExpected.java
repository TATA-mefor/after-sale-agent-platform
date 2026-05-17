package com.example.aftersale.agent.application.evaluation;

import com.example.aftersale.agent.application.planner.SubtaskType;
import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import java.util.List;
import java.util.Objects;

public record EvaluationExpected(
        IntentType intent,
        List<SubtaskType> subtaskTypes,
        List<String> tools,
        ToolRiskLevel riskLevel,
        List<String> policyCategories,
        boolean requiresApproval) {

    public EvaluationExpected {
        intent = Objects.requireNonNull(intent, "intent must not be null");
        subtaskTypes = List.copyOf(Objects.requireNonNull(
                subtaskTypes,
                "subtaskTypes must not be null"));
        tools = List.copyOf(Objects.requireNonNull(tools, "tools must not be null"));
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        policyCategories = List.copyOf(Objects.requireNonNull(
                policyCategories,
                "policyCategories must not be null"));
    }
}
