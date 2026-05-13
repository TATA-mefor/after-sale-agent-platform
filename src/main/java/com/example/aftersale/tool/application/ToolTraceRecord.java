package com.example.aftersale.tool.application;

import com.example.aftersale.tool.domain.ToolInput;
import com.example.aftersale.tool.domain.ToolOutput;
import java.time.Instant;
import java.util.Objects;

public record ToolTraceRecord(String toolName, ToolInput input, ToolOutput output, Instant recordedAt) {

    public ToolTraceRecord {
        Objects.requireNonNull(toolName, "toolName must not be null");
        input = Objects.requireNonNull(input, "input must not be null");
        output = Objects.requireNonNull(output, "output must not be null");
        recordedAt = Objects.requireNonNull(recordedAt, "recordedAt must not be null");
    }
}
