package io.github.tatame.aftersale.agent.application.evaluation;

import io.github.tatame.aftersale.agent.application.planner.SubtaskType;
import io.github.tatame.aftersale.ticket.domain.IntentType;
import io.github.tatame.aftersale.tool.domain.ToolRiskLevel;
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
