package com.example.aftersale.approval.api;

import com.example.aftersale.approval.domain.ApprovalRequest;
import com.example.aftersale.approval.domain.ApprovalStatus;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import java.time.Instant;

public record ApprovalRequestResponse(
        String approvalRequestId,
        String ticketId,
        String agentRunId,
        String subtaskId,
        String toolName,
        String requestedAction,
        ToolRiskLevel riskLevel,
        ApprovalStatus status,
        String reviewerId,
        String decisionReason,
        Instant requestedAt,
        Instant reviewedAt) {

    public static ApprovalRequestResponse from(ApprovalRequest request) {
        return new ApprovalRequestResponse(
                request.getApprovalId(),
                request.getTicketId(),
                request.getRunId(),
                request.getSubtaskId(),
                request.getToolName(),
                request.getRequestedAction(),
                request.getRiskLevel(),
                request.getStatus(),
                request.getReviewerId(),
                request.getDecisionReason(),
                request.getRequestedAt(),
                request.getReviewedAt());
    }
}
