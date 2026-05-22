package com.example.aftersale.agent.application.skill;

import com.example.aftersale.agent.application.handler.SubtaskExecutionResult;
import com.example.aftersale.agent.application.planner.AgentPlan;
import com.example.aftersale.agent.application.planner.AgentSubtask;
import com.example.aftersale.agent.application.workspace.AgentWorkspace;
import com.example.aftersale.ticket.domain.Ticket;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Skill execution context intentionally exposes the current in-run domain objects.")
public record SkillExecutionContext(
        String runId,
        Ticket ticket,
        AgentPlan plan,
        AgentSubtask subtask,
        AgentWorkspace workspace,
        List<String> allowedTools,
        String riskPolicySummary,
        List<SubtaskExecutionResult> previousResults) {

    public SkillExecutionContext {
        runId = requireText(runId, "runId");
        ticket = Objects.requireNonNull(ticket, "ticket must not be null");
        plan = Objects.requireNonNull(plan, "plan must not be null");
        workspace = Objects.requireNonNull(workspace, "workspace must not be null");
        allowedTools = List.copyOf(Objects.requireNonNull(allowedTools, "allowedTools must not be null"));
        riskPolicySummary = requireText(riskPolicySummary, "riskPolicySummary");
        previousResults = List.copyOf(Objects.requireNonNull(previousResults, "previousResults must not be null"));
    }

    public Optional<AgentSubtask> optionalSubtask() {
        return Optional.ofNullable(subtask);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
