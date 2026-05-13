package com.example.aftersale.agent.api;

import com.example.aftersale.agent.application.AgentRunResult;
import com.example.aftersale.agent.domain.AgentRun;
import com.example.aftersale.agent.domain.AgentRunStatus;
import com.example.aftersale.ticket.domain.IntentType;
import java.time.Instant;
import java.util.List;

public record AgentRunResponse(
        String runId,
        String ticketId,
        AgentRunStatus status,
        IntentType intent,
        String plan,
        String finalSuggestion,
        List<String> evidence,
        List<String> toolCalls,
        String errorMessage,
        Instant startedAt,
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
