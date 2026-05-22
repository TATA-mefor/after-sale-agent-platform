package com.example.aftersale.agent.application.planner;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 在 Handler 或工具执行开始前校验 Planner 输出。
 *
 * <p>边界：本校验器拒绝当前 AgentRun 不允许的工具、格式错误的子任务、依赖环和不安全的
 * 高风险完成声明，防止 LLM 或 fallback Planner 绕过 Java 策略。
 */
public final class AgentPlanValidator {

    private static final int MAX_SUBTASKS = 10;

    private static final List<String> UNSAFE_COMPLETION_CLAIMS = List.of(
            "已退款",
            "已完成退款",
            "退款已完成",
            "已补偿",
            "已发放补偿",
            "已关闭争议",
            "争议已关闭",
            "refund completed",
            "refunded",
            "compensation issued",
            "dispute closed");

    private AgentPlanValidator() {
    }

    public static void validate(AgentPlan plan, List<String> availableTools) {
        Set<String> availableToolNames = new HashSet<>(availableTools);
        for (PlannedToolCall plannedTool : plan.plannedTools()) {
            ensureKnownTool(plannedTool, availableToolNames);
        }
        validateSubtasks(plan.subtasks(), availableToolNames);
        ensureSafeText(plan.noteToAdd(), "noteToAdd");
        ensureSafeText(plan.finalSuggestion(), "finalSuggestion");
    }

    private static void validateSubtasks(List<AgentSubtask> subtasks, Set<String> availableToolNames) {
        if (subtasks.size() > MAX_SUBTASKS) {
            throw new AgentPlanValidationException("subtasks must not contain more than " + MAX_SUBTASKS + " items");
        }
        Set<String> subtaskIds = new HashSet<>();
        for (AgentSubtask subtask : subtasks) {
            if (subtask.subtaskId().isBlank()) {
                throw new AgentPlanValidationException("subtaskId must not be blank");
            }
            if (!subtaskIds.add(subtask.subtaskId())) {
                throw new AgentPlanValidationException("Duplicate subtaskId: " + subtask.subtaskId());
            }
            if (subtask.policyQuery().isBlank()) {
                throw new AgentPlanValidationException("Subtask " + subtask.subtaskId()
                        + " policyQuery must not be blank");
            }
            if (subtask.type() == SubtaskType.UNKNOWN) {
                throw new AgentPlanValidationException("Planner returned unknown subtask type: "
                        + subtask.subtaskId());
            }
            for (PlannedToolCall plannedTool : subtask.plannedTools()) {
                ensureKnownTool(plannedTool, availableToolNames);
            }
        }
        for (AgentSubtask subtask : subtasks) {
            for (String dependency : subtask.dependencies()) {
                if (!subtaskIds.contains(dependency)) {
                    throw new AgentPlanValidationException("Subtask " + subtask.subtaskId()
                            + " depends on unknown subtaskId: " + dependency);
                }
            }
        }
        ensureNoDependencyCycles(subtasks);
    }

    private static void ensureKnownTool(PlannedToolCall plannedTool, Set<String> availableToolNames) {
        if (!availableToolNames.contains(plannedTool.toolName())) {
            throw new AgentPlanValidationException(
                    "Planner returned tool not allowed for current AgentRun: " + plannedTool.toolName());
        }
    }

    private static void ensureNoDependencyCycles(List<AgentSubtask> subtasks) {
        Map<String, AgentSubtask> byId = subtasks.stream()
                .collect(Collectors.toMap(AgentSubtask::subtaskId, subtask -> subtask));
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (AgentSubtask subtask : subtasks) {
            visit(subtask.subtaskId(), byId, visiting, visited);
        }
    }

    private static void visit(
            String subtaskId,
            Map<String, AgentSubtask> byId,
            Set<String> visiting,
            Set<String> visited) {
        if (visited.contains(subtaskId)) {
            return;
        }
        if (!visiting.add(subtaskId)) {
            throw new AgentPlanValidationException("Subtask dependencies contain a cycle at: " + subtaskId);
        }
        AgentSubtask subtask = byId.get(subtaskId);
        for (String dependency : subtask.dependencies()) {
            visit(dependency, byId, visiting, visited);
        }
        visiting.remove(subtaskId);
        visited.add(subtaskId);
    }

    private static void ensureSafeText(String value, String fieldName) {
        String normalized = value.toLowerCase();
        for (String unsafeClaim : UNSAFE_COMPLETION_CLAIMS) {
            if (normalized.contains(unsafeClaim)) {
                throw new AgentPlanValidationException(
                        fieldName + " contains an unsafe high-risk completion claim: " + unsafeClaim);
            }
        }
    }
}
