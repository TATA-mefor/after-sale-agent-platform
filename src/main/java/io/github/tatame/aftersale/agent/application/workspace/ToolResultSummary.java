package io.github.tatame.aftersale.agent.application.workspace;

import io.github.tatame.aftersale.tool.domain.ToolExecutionStatus;
import io.github.tatame.aftersale.tool.domain.ToolOutput;
import java.time.Instant;
import java.util.Objects;

/**
 * 保存工具输出的短摘要，供 workspace 汇总使用。
 *
 * <p>边界：这是运行期工作记忆的轻量索引；完整工具入参与输出审计必须继续以 ToolCallTrace 为准。
 */
public record ToolResultSummary(
        String subtaskId,
        String toolName,
        ToolExecutionStatus status,
        String summary,
        Instant createdAt) {

    public ToolResultSummary {
        subtaskId = subtaskId == null ? "" : subtaskId;
        toolName = requireText(toolName, "toolName");
        status = Objects.requireNonNull(status, "status must not be null");
        summary = requireText(summary, "summary");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static ToolResultSummary fromToolOutput(String subtaskId, ToolOutput output, Instant createdAt) {
        return new ToolResultSummary(
                subtaskId,
                output.toolName(),
                output.status(),
                output.message(),
                createdAt);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
