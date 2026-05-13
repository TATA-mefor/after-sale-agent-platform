package com.example.aftersale.tool.application;

import com.example.aftersale.tool.domain.ToolDefinition;
import com.example.aftersale.tool.domain.ToolInput;
import com.example.aftersale.tool.domain.ToolOutput;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ToolRegistry {

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

    public ToolOutput execute(String toolName, ToolInput input) {
        long startedAt = System.nanoTime();
        ToolExecutor executor = executors.get(toolName);
        if (executor == null) {
            ToolOutput output = ToolOutput.failure(toolName, TOOL_NOT_FOUND, "Unknown tool: " + toolName);
            trace(toolName, input, output, startedAt);
            return output;
        }

        ToolDefinition definition = executor.definition();
        if (definition.requiresApproval()) {
            ToolOutput output = ToolOutput.requiresApproval(
                    definition.toolName(),
                    "Tool requires human approval: " + definition.toolName());
            trace(definition.toolName(), input, output, startedAt);
            return output;
        }

        try {
            ToolOutput output = executor.execute(input);
            trace(definition.toolName(), input, output, startedAt);
            return output;
        } catch (RuntimeException exception) {
            ToolOutput output = ToolOutput.failure(
                    definition.toolName(),
                    TOOL_EXECUTION_FAILED,
                    failureMessage(exception));
            trace(definition.toolName(), input, output, startedAt);
            return output;
        }
    }

    private void trace(String toolName, ToolInput input, ToolOutput output, long startedAt) {
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
}
