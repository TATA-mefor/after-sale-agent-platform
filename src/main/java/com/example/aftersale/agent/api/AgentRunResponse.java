package com.example.aftersale.agent.api;

import com.example.aftersale.agent.application.AgentRunResult;
import com.example.aftersale.agent.domain.AgentRun;
import com.example.aftersale.agent.domain.AgentRunStatus;
import com.example.aftersale.ticket.domain.IntentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "AgentRun result with concise evidence and tool-call summaries.")
public record AgentRunResponse(
        @Schema(description = "AgentRun id.", example = "RUN-DEMO-1001")
        String runId,
        @Schema(description = "Ticket id processed by this run.", example = "T-DEMO-1001")
        String ticketId,
        @Schema(description = "AgentRun status.")
        AgentRunStatus status,
        @Schema(description = "Intent used by the planner and specialist handler.")
        IntentType intent,
        @Schema(description = "Planner summary. The planner never executes tools directly.")
        String plan,
        @Schema(description = "Final suggestion. It must not claim completed refunds, exchanges, or payments.")
        String finalSuggestion,
        @Schema(description = "Short policy evidence summaries.")
        List<String> evidence,
        @Schema(description = "Tool call summaries recorded through ToolRegistry.")
        List<String> toolCalls,
        @Schema(description = "Error message when the run failed.")
        String errorMessage,
        @Schema(description = "Run start time.")
        Instant startedAt,
        @Schema(description = "Run finish time.")
        Instant finishedAt) {

    public AgentRunResponse {
        evidence = List.copyOf(evidence);
        toolCalls = List.copyOf(toolCalls);
    }

    @Override
    public List<String> evidence() {
        return List.copyOf(evidence);
    }

    @Override
    public List<String> toolCalls() {
        return List.copyOf(toolCalls);
    }

    public static AgentRunResponse from(AgentRunResult result) {
        AgentRun agentRun = result.agentRun();
        return new AgentRunResponse(
                agentRun.getRunId(),
                agentRun.getTicketId(),
                agentRun.getStatus(),
                result.intent(),
                result.plan(),
                result.finalSuggestion(),
                result.evidence(),
                result.toolCalls(),
                agentRun.getErrorMessage(),
                agentRun.getStartedAt(),
                agentRun.getFinishedAt());
    }
}
