package com.example.aftersale.agent.application.skill;

import com.example.aftersale.agent.application.planner.SubtaskType;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record SkillDefinition(
        String skillName,
        String description,
        Set<SubtaskType> supportedSubtaskTypes,
        List<String> requiredTools,
        List<String> optionalTools,
        ToolRiskLevel riskLevel,
        String requiresApprovalWhen,
        List<String> evidenceRequirements,
        List<String> workspaceReads,
        List<String> workspaceWrites) {

    public SkillDefinition {
        skillName = requireText(skillName, "skillName");
        description = requireText(description, "description");
        supportedSubtaskTypes = Set.copyOf(Objects.requireNonNull(
                supportedSubtaskTypes, "supportedSubtaskTypes must not be null"));
        if (supportedSubtaskTypes.isEmpty()) {
            throw new IllegalArgumentException("supportedSubtaskTypes must not be empty");
        }
        requiredTools = List.copyOf(Objects.requireNonNull(requiredTools, "requiredTools must not be null"));
        optionalTools = List.copyOf(Objects.requireNonNull(optionalTools, "optionalTools must not be null"));
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        requiresApprovalWhen = normalize(requiresApprovalWhen);
        evidenceRequirements = List.copyOf(Objects.requireNonNull(
                evidenceRequirements, "evidenceRequirements must not be null"));
        workspaceReads = List.copyOf(Objects.requireNonNull(workspaceReads, "workspaceReads must not be null"));
        workspaceWrites = List.copyOf(Objects.requireNonNull(workspaceWrites, "workspaceWrites must not be null"));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
