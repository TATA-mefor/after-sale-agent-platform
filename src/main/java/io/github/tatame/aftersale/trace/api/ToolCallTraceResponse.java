package io.github.tatame.aftersale.trace.api;

import io.github.tatame.aftersale.trace.domain.ToolCallStatus;
import io.github.tatame.aftersale.trace.domain.ToolCallTrace;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Tool call audit response. JSON fields are sanitized summaries, not raw prompts or secrets.")
public record ToolCallTraceResponse(
        @Schema(description = "Trace id.", example = "TRACE-DEMO-1001")
        String traceId,
        @Schema(description = "AgentRun id.", example = "RUN-DEMO-1001")
        String runId,
        @Schema(description = "Tool name such as search_aftersale_policy.", example = "search_aftersale_policy")
        String toolName,
        @Schema(description = "Tool input JSON recorded for audit.")
        String inputJson,
        @Schema(description = "Tool output JSON. RAG evidence appears here when policy search is used.")
        String outputJson,
        @Schema(description = "Tool call status.")
        ToolCallStatus status,
        @Schema(description = "Observed tool latency in milliseconds.", example = "12")
        long latencyMs,
        @Schema(description = "Failure message when status is failure.")
        String errorMessage,
        @Schema(description = "Trace creation time.")
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
