package com.example.aftersale.agent.application.skill;

import java.util.List;
import java.util.Objects;

public record SkillExecutionResult(
        String skillName,
        SkillExecutionStatus status,
        String summary,
        List<String> evidence,
        List<String> toolCalls,
        boolean approvalRequired,
        String errorCode,
        String errorMessage) {

    public SkillExecutionResult {
        skillName = requireText(skillName, "skillName");
        status = Objects.requireNonNull(status, "status must not be null");
        summary = requireText(summary, "summary");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence must not be null"));
        toolCalls = List.copyOf(Objects.requireNonNull(toolCalls, "toolCalls must not be null"));
        errorCode = errorCode == null ? "" : errorCode;
        errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public static SkillExecutionResult succeeded(
            String skillName,
            String summary,
            List<String> evidence,
            List<String> toolCalls) {
        return new SkillExecutionResult(
                skillName,
                SkillExecutionStatus.SUCCEEDED,
                summary,
                evidence,
                toolCalls,
                false,
                "",
                "");
    }

    public static SkillExecutionResult failed(String skillName, String errorCode, String errorMessage) {
        return new SkillExecutionResult(
                skillName,
                SkillExecutionStatus.FAILED,
                "Skill " + skillName + " failed: " + requireText(errorMessage, "errorMessage"),
                List.of(),
                List.of(),
                false,
                requireText(errorCode, "errorCode"),
                errorMessage);
    }

    public static SkillExecutionResult waitingApproval(String skillName, String summary) {
        return new SkillExecutionResult(
                skillName,
                SkillExecutionStatus.WAITING_APPROVAL,
                summary,
                List.of(),
                List.of(),
                true,
                "",
                "");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
