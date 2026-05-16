package com.example.aftersale.agent.application.workspace;

import com.example.aftersale.tool.domain.ToolRiskLevel;
import java.util.Objects;

public record RiskFlag(
        String subtaskId,
        ToolRiskLevel riskLevel,
        String reason) {

    public RiskFlag {
        subtaskId = subtaskId == null ? "" : subtaskId;
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        reason = requireText(reason, "reason");
    }

    public String summary() {
        String prefix = subtaskId.isBlank() ? "" : subtaskId + " ";
        return prefix + riskLevel.name() + ": " + reason;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
