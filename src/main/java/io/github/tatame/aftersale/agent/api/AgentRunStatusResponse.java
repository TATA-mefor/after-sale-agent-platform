package io.github.tatame.aftersale.agent.api;

import io.github.tatame.aftersale.agent.domain.AgentRun;
import io.github.tatame.aftersale.agent.domain.AgentRunStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Read-only AgentRun status summary for polling and navigation to trace views.")
public record AgentRunStatusResponse(
        @Schema(description = "AgentRun id.", example = "RUN-DEMO-1001")
        String runId,
        @Schema(description = "Ticket id processed by this run.", example = "T-DEMO-1001")
        String ticketId,
        @Schema(description = "Current AgentRun status.")
        AgentRunStatus status,
        @Schema(description = "Run start time.")
        Instant startedAt,
        @Schema(description = "Run completion time when the run has finished.")
        Instant completedAt,
        @Schema(description = "Short final summary. This does not include planner JSON, trace JSON, or workspace data.")
        String finalSummary,
        @Schema(description = "Failure summary when the run failed or was cancelled.")
        String failureSummary,
        @Schema(description = "Whether the ToolCallTrace read-only view can be queried for this persisted run.")
        boolean traceAvailable,
        @Schema(description = "Whether the Execution Tree read-only view can be queried for this persisted run.")
        boolean executionTreeAvailable,
        @Schema(description = "ToolCallTrace read-only API path for this run.")
        String traceUrl,
        @Schema(description = "Execution Tree read-only API path for this run.")
        String executionTreeUrl) {

    public static AgentRunStatusResponse from(AgentRun agentRun) {
        String runId = agentRun.getRunId();
        return new AgentRunStatusResponse(
                runId,
                agentRun.getTicketId(),
                agentRun.getStatus(),
                agentRun.getStartedAt(),
                agentRun.getFinishedAt(),
                agentRun.getFinalAnswer(),
                agentRun.getErrorMessage(),
                true,
                true,
                "/api/agent-runs/" + runId + "/traces",
                "/api/agent-runs/" + runId + "/execution-tree");
    }
}
