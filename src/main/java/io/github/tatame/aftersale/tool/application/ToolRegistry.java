package io.github.tatame.aftersale.tool.application;

import io.github.tatame.aftersale.common.observability.MdcScope;
import io.github.tatame.aftersale.common.observability.ObservabilityConstants;
import io.github.tatame.aftersale.tool.domain.ToolDefinition;
import io.github.tatame.aftersale.tool.domain.ToolInput;
import io.github.tatame.aftersale.tool.domain.ToolOutput;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 路由所有工具调用，并为可审计的 Agent 执行记录 trace 元数据。
   所有工具调用都必须走 ToolRegistry，例如：
        get_order_by_id
        search_aftersale_policy
        add_ticket_note
 * <p>边界：调用方必须使用本注册表，而不是直接调用 ToolExecutor Bean。注册表返回结构化失败和需要审批的结果，
 * 但不拥有业务状态，也不把 trace 数据变成领域事实来源。
 */
@Component
public class ToolRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolRegistry.class);

    private static final String TOOL_NOT_FOUND = "TOOL_NOT_FOUND";
    private static final String TOOL_EXECUTION_FAILED = "TOOL_EXECUTION_FAILED";

    private final Map<String, ToolExecutor> executors;
    private final ToolTraceRecorder traceRecorder;

    @SuppressFBWarnings(
            value = "CT_CONSTRUCTOR_THROW",
            justification = "The registry rejects duplicate tool names during Spring bean construction.")
    public ToolRegistry(List<ToolExecutor> toolExecutors, ToolTraceRecorder traceRecorder) {
        this.executors = Map.copyOf(indexByToolName(toolExecutors));
        this.traceRecorder = traceRecorder;
    }

    public Optional<ToolDefinition> findDefinition(String toolName) {
        ToolExecutor executor = executors.get(toolName);
        if (executor == null) {
            return Optional.empty();
        }
        return Optional.of(executor.definition());
    }

    public List<ToolDefinition> listDefinitions() {
        return executors.values().stream()
                .map(ToolExecutor::definition)
                .toList();
    }

    /**
     * 执行一个已注册工具，并在存在 AgentRun 上下文时记录 ToolCallTrace。
     *
     * <p>未知工具、需要审批的工具和执行器异常都会转换为 ToolOutput，使 AgentRun 能暴露失败而不隐藏调用尝试。
     */
    public ToolOutput execute(String toolName, ToolInput input) {
        long startedAt = System.nanoTime();
        try (MdcScope ignored = MdcScope.putAll(toolMdcValues(toolName, input))) {
            LOGGER.info("tool.invocation.started");
            ToolExecutor executor = executors.get(toolName);
            if (executor == null) {
                ToolOutput output = ToolOutput.failure(toolName, TOOL_NOT_FOUND, "Unknown tool: " + toolName);
                trace(toolName, input, output, startedAt);
                logToolCompleted(output, startedAt);
                return output;
            }

            ToolDefinition definition = executor.definition();
            if (definition.requiresApproval()) {
                ToolOutput output = ToolOutput.requiresApproval(
                        definition.toolName(),
                        "Tool requires human approval: " + definition.toolName());
                trace(definition.toolName(), input, output, startedAt);
                logToolCompleted(output, startedAt);
                return output;
            }

            try {
                ToolOutput output = executor.execute(input);
                trace(definition.toolName(), input, output, startedAt);
                logToolCompleted(output, startedAt);
                return output;
            } catch (RuntimeException exception) {
                ToolOutput output = ToolOutput.failure(
                        definition.toolName(),
                        TOOL_EXECUTION_FAILED,
                        failureMessage(exception));
                trace(definition.toolName(), input, output, startedAt);
                LOGGER.warn("tool.invocation.failed status={} latencyMs={} errorType={}",
                        output.status(),
                        elapsedMillis(startedAt),
                        exception.getClass().getSimpleName());
                return output;
            }
        }
    }

    private void trace(String toolName, ToolInput input, ToolOutput output, long startedAt) {
        // AgentRun 外部调用仍可执行，但只有带 run 上下文的调用会进入审计 trace。
        ToolTraceContext.currentRunId()
                .ifPresent(runId -> traceRecorder.record(new ToolTraceRecord(
                        runId,
                        toolName,
                        input,
                        output,
                        elapsedMillis(startedAt),
                        Instant.now())));
    }

    private static long elapsedMillis(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private static Map<String, ToolExecutor> indexByToolName(List<ToolExecutor> toolExecutors) {
        Map<String, ToolExecutor> indexedExecutors = new LinkedHashMap<>();
        for (ToolExecutor executor : toolExecutors) {
            String toolName = executor.definition().toolName();
            ToolExecutor previous = indexedExecutors.putIfAbsent(toolName, executor);
            if (previous != null) {
                throw new IllegalStateException("Duplicate toolName: " + toolName);
            }
        }
        return indexedExecutors;
    }

    private static String failureMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }

    private static Map<String, Object> toolMdcValues(String toolName, ToolInput input) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(ObservabilityConstants.TOOL_NAME, toolName);
        input.optionalString("ticketId").ifPresent(value -> values.put(ObservabilityConstants.TICKET_ID, value));
        input.optionalString("subtaskId").ifPresent(value -> values.put(ObservabilityConstants.SUBTASK_ID, value));
        ToolTraceContext.currentRunId()
                .ifPresent(value -> values.put(ObservabilityConstants.AGENT_RUN_ID, value));
        return values;
    }

    private static void logToolCompleted(ToolOutput output, long startedAt) {
        LOGGER.info("tool.invocation.completed status={} latencyMs={}", output.status(), elapsedMillis(startedAt));
    }
}
