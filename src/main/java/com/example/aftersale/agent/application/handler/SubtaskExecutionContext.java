package com.example.aftersale.agent.application.handler;

import com.example.aftersale.agent.application.planner.AgentPlan;
import com.example.aftersale.agent.application.planner.AgentSubtask;
import com.example.aftersale.ticket.domain.Ticket;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;

@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "The execution context intentionally exposes the current in-memory Ticket to handlers.")
public record SubtaskExecutionContext(
        String runId,
        Ticket ticket,
        AgentPlan agentPlan,
        AgentSubtask subtask,
        List<String> availableTools,
        String riskPolicySummary,
        List<SubtaskExecutionResult> previousResults) {

    public SubtaskExecutionContext {
        runId = requireText(runId, "runId");
        ticket = Objects.requireNonNull(ticket, "ticket must not be null");
        agentPlan = Objects.requireNonNull(agentPlan, "agentPlan must not be null");
        subtask = Objects.requireNonNull(subtask, "subtask must not be null");
        availableTools = List.copyOf(Objects.requireNonNull(availableTools, "availableTools must not be null"));
        riskPolicySummary = requireText(riskPolicySummary, "riskPolicySummary");
        previousResults = List.copyOf(Objects.requireNonNull(previousResults, "previousResults must not be null"));
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
