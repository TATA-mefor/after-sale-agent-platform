package io.github.tatame.aftersale.agent.application.workspace;

import io.github.tatame.aftersale.tool.domain.ToolRiskLevel;
import java.util.Objects;

/**
 * 记录本次 AgentRun 中被识别出的风险提示。
 *
 * <p>边界：风险提示只能辅助编排和审批判断，不能绕过 RiskPolicy 或直接授权高风险业务动作。
 */
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
