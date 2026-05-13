package com.example.aftersale.trace.api;

import com.example.aftersale.trace.domain.ToolCallStatus;
import com.example.aftersale.trace.domain.ToolCallTrace;
import java.time.Instant;

public record ToolCallTraceResponse(
        String traceId,
        String runId,
        String toolName,
        String inputJson,
        String outputJson,
        ToolCallStatus status,
        long latencyMs,
        String errorMessage,
        Instant createdAt) {

    public static ToolCallTraceResponse from(ToolCallTrace trace) {
        return new ToolCallTraceResponse(
                trace.getTraceId(),
                trace.getRunId(),
                trace.getToolName(),
                trace.getInputJson(),
                trace.getOutputJson(),
                trace.getStatus(),
                trace.getLatencyMs(),
                trace.getErrorMessage(),
                trace.getCreatedAt());
    }
}
