package com.example.aftersale.ticket.domain;

import java.time.Instant;
import java.util.Objects;

public final class Ticket {

    private static final String DEFAULT_PRIORITY = "NORMAL";

    private final String ticketId;
    private final String userId;
    private final String orderId;
    private final String rawUserMessage;
    private final Instant createdAt;
    private IntentType intentType;
    private String priority;
    private TicketStatus status;
    private String internalNote;
    private String agentSuggestion;
    private Instant updatedAt;

    private Ticket(String ticketId, String userId, String orderId, String rawUserMessage, Instant createdAt) {
        this.ticketId = requireText(ticketId, "ticketId");
        this.userId = requireText(userId, "userId");
        this.orderId = requireText(orderId, "orderId");
        this.rawUserMessage = requireText(rawUserMessage, "rawUserMessage");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.intentType = IntentType.UNKNOWN;
        this.priority = DEFAULT_PRIORITY;
        this.status = TicketStatus.CREATED;
        this.updatedAt = createdAt;
    }

    public static Ticket create(String ticketId, String userId, String orderId, String rawUserMessage, Instant now) {
        return new Ticket(ticketId, userId, orderId, rawUserMessage, now);
    }

    public void classifyIntent(IntentType newIntentType, Instant changedAt) {
        this.intentType = Objects.requireNonNull(newIntentType, "intentType must not be null");
        touch(changedAt);
    }

    public void changePriority(String newPriority, Instant changedAt) {
        this.priority = requireText(newPriority, "priority");
        touch(changedAt);
    }

    public void addInternalNote(String note, Instant changedAt) {
        this.internalNote = requireText(note, "note");
        touch(changedAt);
    }

    public void startAgentRun(Instant changedAt) {
        ensureNotTerminal();
        changeStatus(TicketStatus.AGENT_RUNNING, changedAt);
    }

    public void waitForUserInfo(Instant changedAt) {
        ensureNotTerminal();
        changeStatus(TicketStatus.WAITING_USER_INFO, changedAt);
    }

    public void waitForHumanApproval(Instant changedAt) {
        ensureNotTerminal();
        changeStatus(TicketStatus.WAITING_HUMAN_APPROVAL, changedAt);
    }

    public void startProcessing(Instant changedAt) {
        ensureNotTerminal();
        changeStatus(TicketStatus.PROCESSING, changedAt);
    }

    public void resolve(String suggestion, Instant changedAt) {
        ensureNotTerminal();
        this.agentSuggestion = requireText(suggestion, "suggestion");
        changeStatus(TicketStatus.RESOLVED, changedAt);
    }

    public void reject(String suggestion, Instant changedAt) {
        ensureNotTerminal();
        this.agentSuggestion = requireText(suggestion, "suggestion");
        changeStatus(TicketStatus.REJECTED, changedAt);
    }

    public void fail(String errorMessage, Instant changedAt) {
        ensureNotTerminal();
        this.agentSuggestion = requireText(errorMessage, "errorMessage");
        changeStatus(TicketStatus.FAILED, changedAt);
    }

    public void close(Instant changedAt) {
        if (status != TicketStatus.RESOLVED && status != TicketStatus.REJECTED) {
            throw new IllegalStateException("Only resolved or rejected tickets can be closed");
        }
        changeStatus(TicketStatus.CLOSED, changedAt);
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getUserId() {
        return userId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getRawUserMessage() {
        return rawUserMessage;
    }

    public IntentType getIntentType() {
        return intentType;
    }

    public String getPriority() {
        return priority;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public String getAgentSuggestion() {
        return agentSuggestion;
    }

    public String getInternalNote() {
        return internalNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void changeStatus(TicketStatus newStatus, Instant changedAt) {
        this.status = Objects.requireNonNull(newStatus, "status must not be null");
        touch(changedAt);
    }

    private void ensureNotTerminal() {
        if (status == TicketStatus.CLOSED || status == TicketStatus.FAILED) {
            throw new IllegalStateException("Ticket is already terminal: " + status);
        }
    }

    private void touch(Instant changedAt) {
        this.updatedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
