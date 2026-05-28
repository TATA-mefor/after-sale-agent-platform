package com.example.aftersale.approval.api;

import com.example.aftersale.approval.domain.ApprovalRequest;
import com.example.aftersale.approval.domain.ApprovalStatus;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Approval request audit response for a high-risk proposed action.")
public record ApprovalRequestResponse(
        @Schema(description = "Approval request id.", example = "APR-DEMO-1001")
        String approvalRequestId,
        @Schema(description = "Related ticket id.", example = "T-DEMO-1001")
        String ticketId,
        @Schema(description = "Related AgentRun id.", example = "RUN-DEMO-1001")
        String agentRunId,
        @Schema(description = "Related subtask id.")
        String subtaskId,
        @Schema(description = "Tool name that requested approval.")
        String toolName,
        @Schema(description = "Requested action summary. It is not executed until approved by runtime policy.")
        String requestedAction,
        @Schema(description = "Tool risk level.")
        ToolRiskLevel riskLevel,
        @Schema(description = "Approval status.")
        ApprovalStatus status,
        @Schema(description = "Reviewer id when reviewed.")
        String reviewerId,
        @Schema(description = "Decision reason when reviewed.")
        String decisionReason,
        @Schema(description = "Request creation time.")
        Instant requestedAt,
        @Schema(description = "Review time.")
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
