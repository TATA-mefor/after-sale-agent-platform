package io.github.tatame.aftersale.agent.application.workspace;

import io.github.tatame.aftersale.agent.application.planner.SubtaskStatus;
import io.github.tatame.aftersale.agent.application.planner.SubtaskType;
import java.util.List;
import java.util.Objects;

/**
 * 保存一个子任务在本次 AgentRun 内的执行摘要。
 *
 * <p>边界：该记录帮助 final summary 复用结构化结果，但长期记忆、跨会话画像和审计事实仍不属于 workspace。
 */
public record SubtaskMemory(
        String subtaskId,
        SubtaskType subtaskType,
        String target,
        SubtaskStatus status,
        String summary,
        List<String> relatedToolNames) {

    public SubtaskMemory {
        subtaskId = requireText(subtaskId, "subtaskId");
        subtaskType = Objects.requireNonNull(subtaskType, "subtaskType must not be null");
        target = requireText(target, "target");
        status = Objects.requireNonNull(status, "status must not be null");
        summary = requireText(summary, "summary");
        relatedToolNames = List.copyOf(Objects.requireNonNull(
                relatedToolNames,
                "relatedToolNames must not be null"));
    }

    public String summary() {
        return subtaskId + " " + subtaskType.name() + " " + status.name() + ": " + summary;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
