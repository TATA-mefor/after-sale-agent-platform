package io.github.tatame.aftersale.agent.application.executiontree;

import io.github.tatame.aftersale.approval.domain.ApprovalStatus;
import java.time.Instant;
import java.util.Objects;

public record ExecutionTreeApprovalNode(
        String approvalRequestId,
        ApprovalStatus status,
        String reason,
        String decisionReason,
        Instant createdAt,
        Instant decidedAt) {

    public ExecutionTreeApprovalNode {
        approvalRequestId = requireText(approvalRequestId, "approvalRequestId");
        status = Objects.requireNonNull(status, "status must not be null");
        reason = requireText(reason, "reason");
        decisionReason = decisionReason == null ? "" : decisionReason;
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
