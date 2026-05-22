package com.example.aftersale.trace.domain;

import java.time.Instant;
import java.util.Objects;
// 每次工具调用都会记录 trace（调用了谁、入参摘要、结果、耗时、成功/失败等）。
public final class ToolCallTrace {

    private final String traceId;
    private final String runId;
    private final String toolName;
    private final String inputJson;
    private final Instant createdAt;
    private ToolCallStatus status;
    private String outputJson;
    private long latencyMs;
    private String errorMessage;

    private ToolCallTrace(String traceId, String runId, String toolName, String inputJson, Instant createdAt) {
        this.traceId = requireText(traceId, "traceId");
        this.runId = requireText(runId, "runId");
        this.toolName = requireText(toolName, "toolName");
        this.inputJson = requireText(inputJson, "inputJson");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.status = ToolCallStatus.RUNNING;
    }

    public static ToolCallTrace start(
            String traceId,
            String runId,
            String toolName,
            String inputJson,
            Instant createdAt) {
        return new ToolCallTrace(traceId, runId, toolName, inputJson, createdAt);
    }

    public static ToolCallTrace restore(
            String traceId,
            String runId,
            String toolName,
            String inputJson,
            ToolCallStatus status,
            String outputJson,
            long latencyMs,
            String errorMessage,
            Instant createdAt) {
        ToolCallTrace trace = new ToolCallTrace(traceId, runId, toolName, inputJson, createdAt);
        trace.status = Objects.requireNonNull(status, "status must not be null");
        trace.outputJson = outputJson;
        trace.latencyMs = requireNonNegativeLatency(latencyMs);
        trace.errorMessage = errorMessage;
        return trace;
    }

    public void markSucceeded(String successfulOutputJson, long successfulLatencyMs) {
        ensureRunning();
        this.outputJson = requireText(successfulOutputJson, "outputJson");
        this.latencyMs = requireNonNegativeLatency(successfulLatencyMs);
        this.errorMessage = null;
        this.status = ToolCallStatus.SUCCEEDED;
    }

    public void markFailed(String failureMessage, long failedLatencyMs) {
        ensureRunning();
        this.errorMessage = requireText(failureMessage, "errorMessage");
        this.latencyMs = requireNonNegativeLatency(failedLatencyMs);
        this.status = ToolCallStatus.FAILED;
    }

    public void markRequiresApproval(String approvalOutputJson, long approvalLatencyMs) {
        ensureRunning();
        this.outputJson = requireText(approvalOutputJson, "outputJson");
        this.latencyMs = requireNonNegativeLatency(approvalLatencyMs);
        this.status = ToolCallStatus.REQUIRES_APPROVAL;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getRunId() {
        return runId;
    }

    public String getToolName() {
        return toolName;
    }

    public String getInputJson() {
        return inputJson;
    }

    public String getOutputJson() {
        return outputJson;
    }

    public ToolCallStatus getStatus() {
        return status;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private void ensureRunning() {
        if (status != ToolCallStatus.RUNNING) {
            throw new IllegalStateException("ToolCallTrace is not running: " + status);
        }
    }

    private static long requireNonNegativeLatency(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("latencyMs must not be negative");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
