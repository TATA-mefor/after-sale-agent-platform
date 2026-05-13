package com.example.aftersale.tool.application;

import com.example.aftersale.tool.domain.ToolInput;
import com.example.aftersale.tool.domain.ToolOutput;
import java.time.Instant;
import java.util.Objects;

public record ToolTraceRecord(String runId, String toolName, ToolInput input, ToolOutput output, long latencyMs,
                              Instant recordedAt) {

    public ToolTraceRecord {
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(toolName, "toolName must not be null");
        input = Objects.requireNonNull(input, "input must not be null");
        output = Objects.requireNonNull(output, "output must not be null");
        if (latencyMs < 0) {
            throw new IllegalArgumentException("latencyMs must not be negative");
        }
        recordedAt = Objects.requireNonNull(recordedAt, "recordedAt must not be null");
    }
}
