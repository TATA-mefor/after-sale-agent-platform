package com.example.aftersale.approval.domain;

import com.example.aftersale.tool.domain.ToolRiskLevel;
import java.time.Instant;
import java.util.Objects;

public final class ApprovalRequest {

    private final String approvalId;
    private final String ticketId;
    private final String runId;
    private final String subtaskId;
    private final String toolName;
    private final String requestedAction;
    private final ToolRiskLevel riskLevel;
    private final Instant requestedAt;
    private ApprovalStatus status;
    private String reviewerId;
    private String decisionReason;
    private Instant reviewedAt;

    private ApprovalRequest(
            String approvalId,
            String ticketId,
            String runId,
            String subtaskId,
            String toolName,
            String requestedAction,
            ToolRiskLevel riskLevel,
            Instant requestedAt) {
        this.approvalId = requireText(approvalId, "approvalId");
        this.ticketId = requireText(ticketId, "ticketId");
        this.runId = requireText(runId, "runId");
        this.subtaskId = subtaskId == null ? "" : subtaskId;
        this.toolName = requireText(toolName, "toolName");
        this.requestedAction = requireText(requestedAction, "requestedAction");
        this.riskLevel = requireApprovalRisk(riskLevel);
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        this.status = ApprovalStatus.PENDING;
    }

    public static ApprovalRequest createForHighRiskTool(
            String approvalId,
            String ticketId,
            String runId,
            String toolName,
            String requestedAction,
            ToolRiskLevel riskLevel,
            Instant requestedAt) {
        return createForHighRiskTool(
                approvalId,
                ticketId,
                runId,
                "",
                toolName,
                requestedAction,
                riskLevel,
                requestedAt);
    }

    public static ApprovalRequest createForHighRiskTool(
            String approvalId,
            String ticketId,
            String runId,
            String subtaskId,
            String toolName,
            String requestedAction,
            ToolRiskLevel riskLevel,
            Instant requestedAt) {
        return new ApprovalRequest(
                approvalId,
                ticketId,
                runId,
                subtaskId,
                toolName,
                requestedAction,
                riskLevel,
                requestedAt);
    }

    public void approve(String approvedBy, String reason, Instant approvedAt) {
        ensurePending();
        this.reviewerId = requireText(approvedBy, "approvedBy");
        this.decisionReason = requireText(reason, "reason");
        this.reviewedAt = requireNotBeforeRequest(approvedAt);
        this.status = ApprovalStatus.APPROVED;
    }

    public void reject(String rejectedBy, String reason, Instant rejectedAt) {
        ensurePending();
        this.reviewerId = requireText(rejectedBy, "rejectedBy");
        this.decisionReason = requireText(reason, "reason");
        this.reviewedAt = requireNotBeforeRequest(rejectedAt);
        this.status = ApprovalStatus.REJECTED;
    }

    public void cancel(String reason, Instant cancelledAt) {
        ensurePending();
        this.decisionReason = requireText(reason, "reason");
        this.reviewedAt = requireNotBeforeRequest(cancelledAt);
        this.status = ApprovalStatus.CANCELLED;
    }

    public String getApprovalId() {
        return approvalId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getRunId() {
        return runId;
    }

    public String getSubtaskId() {
        return subtaskId;
    }

    public String getToolName() {
        return toolName;
    }

    public String getRequestedAction() {
        return requestedAction;
    }

    public ToolRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public boolean requiresApproval() {
        return riskLevel.requiresApproval();
    }

    private void ensurePending() {
        if (status != ApprovalStatus.PENDING) {
            throw new IllegalStateException("ApprovalRequest is not pending: " + status);
        }
    }

    private Instant requireNotBeforeRequest(Instant value) {
        Objects.requireNonNull(value, "reviewedAt must not be null");
        if (value.isBefore(requestedAt)) {
            throw new IllegalArgumentException("reviewedAt must not be before requestedAt");
        }
        return value;
    }

    private static ToolRiskLevel requireApprovalRisk(ToolRiskLevel value) {
        Objects.requireNonNull(value, "riskLevel must not be null");
        if (!value.requiresApproval()) {
            throw new IllegalArgumentException("ApprovalRequest can only be created for approval-required tools");
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
