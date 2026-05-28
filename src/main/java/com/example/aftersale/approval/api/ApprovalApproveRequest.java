package com.example.aftersale.approval.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Human approval decision request.")
public record ApprovalApproveRequest(
        @Schema(description = "Synthetic reviewer id.", example = "reviewer-demo")
        String reviewerId,
        @Schema(description = "Short reason for audit.", example = "Policy evidence supports manual review.")
        String reason) {
}
