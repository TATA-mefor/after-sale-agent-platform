package io.github.tatame.aftersale.approval.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Human rejection decision request.")
public record ApprovalRejectRequest(
        @Schema(description = "Synthetic reviewer id.", example = "reviewer-demo")
        String reviewerId,
        @Schema(description = "Short reason for audit.", example = "Evidence is insufficient for escalation.")
        String reason) {
}
