package com.example.aftersale.agent.domain;

import java.time.Instant;
import java.util.Objects;

public final class AgentRun {

    private final String runId;
    private final String ticketId;
    private final Instant startedAt;
    private AgentRunStatus status;
    private String planJson;
    private String finalAnswer;
    private String errorMessage;
    private Instant finishedAt;

    private AgentRun(String runId, String ticketId, Instant startedAt) {
        this.runId = requireText(runId, "runId");
        this.ticketId = requireText(ticketId, "ticketId");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        this.status = AgentRunStatus.RUNNING;
    }

    public static AgentRun start(String runId, String ticketId, Instant startedAt) {
        return new AgentRun(runId, ticketId, startedAt);
    }

    public static AgentRun restore(
            String runId,
            String ticketId,
            AgentRunStatus status,
            String planJson,
            String finalAnswer,
            String errorMessage,
            Instant startedAt,
            Instant finishedAt) {
        AgentRun agentRun = new AgentRun(runId, ticketId, startedAt);
        agentRun.status = Objects.requireNonNull(status, "status must not be null");
        agentRun.planJson = planJson;
        agentRun.finalAnswer = finalAnswer;
        agentRun.errorMessage = errorMessage;
        agentRun.finishedAt = finishedAt;
        return agentRun;
    }

    public void succeed(String completedPlanJson, String completedFinalAnswer, Instant completedAt) {
        ensureRunning();
        this.planJson = requireText(completedPlanJson, "planJson");
        this.finalAnswer = requireText(completedFinalAnswer, "finalAnswer");
        this.errorMessage = null;
        this.finishedAt = requireNotBeforeStart(completedAt);
        this.status = AgentRunStatus.SUCCEEDED;
    }

    public void fail(String failureMessage, Instant failedAt) {
        ensureRunning();
        this.errorMessage = requireText(failureMessage, "errorMessage");
        this.finishedAt = requireNotBeforeStart(failedAt);
        this.status = AgentRunStatus.FAILED;
    }

    public void cancel(String cancellationReason, Instant cancelledAt) {
        ensureRunning();
        this.errorMessage = requireText(cancellationReason, "cancellationReason");
        this.finishedAt = requireNotBeforeStart(cancelledAt);
        this.status = AgentRunStatus.CANCELLED;
    }

    public String getRunId() {
        return runId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public AgentRunStatus getStatus() {
        return status;
    }

    public String getPlanJson() {
        return planJson;
    }

    public String getFinalAnswer() {
        return finalAnswer;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    private void ensureRunning() {
        if (status != AgentRunStatus.RUNNING) {
            throw new IllegalStateException("AgentRun is not running: " + status);
        }
    }

    private Instant requireNotBeforeStart(Instant value) {
        Objects.requireNonNull(value, "finishedAt must not be null");
        if (value.isBefore(startedAt)) {
            throw new IllegalArgumentException("finishedAt must not be before startedAt");
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
