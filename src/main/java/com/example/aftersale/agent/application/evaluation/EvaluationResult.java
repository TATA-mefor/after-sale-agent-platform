package com.example.aftersale.agent.application.evaluation;

import com.example.aftersale.agent.application.planner.SubtaskType;
import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import java.util.List;
import java.util.Objects;

public record EvaluationResult(
        String caseId,
        boolean passed,
        boolean planValid,
        IntentType actualIntent,
        List<SubtaskType> actualSubtaskTypes,
        List<String> actualTools,
        ToolRiskLevel actualRiskLevel,
        List<String> actualPolicyCategories,
        boolean actualRequiresApproval,
        List<EvaluationFailure> failures) {

    public EvaluationResult {
        caseId = requireText(caseId, "caseId");
        actualIntent = Objects.requireNonNull(actualIntent, "actualIntent must not be null");
        actualSubtaskTypes = List.copyOf(Objects.requireNonNull(
                actualSubtaskTypes,
                "actualSubtaskTypes must not be null"));
        actualTools = List.copyOf(Objects.requireNonNull(actualTools, "actualTools must not be null"));
        actualRiskLevel = Objects.requireNonNull(actualRiskLevel, "actualRiskLevel must not be null");
        actualPolicyCategories = List.copyOf(Objects.requireNonNull(
                actualPolicyCategories,
                "actualPolicyCategories must not be null"));
        failures = List.copyOf(Objects.requireNonNull(failures, "failures must not be null"));
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
