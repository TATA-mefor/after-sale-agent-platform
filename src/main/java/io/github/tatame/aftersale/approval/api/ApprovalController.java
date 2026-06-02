package io.github.tatame.aftersale.approval.api;

import io.github.tatame.aftersale.approval.application.ApprovalApplicationService;
import io.github.tatame.aftersale.common.api.ApiResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/approval-requests")
@Tag(name = "Approvals", description = "Approval-gated handling for high-risk tool actions.")
public class ApprovalController {

    private final ApprovalApplicationService approvalApplicationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the application service dependency.")
    public ApprovalController(ApprovalApplicationService approvalApplicationService) {
        this.approvalApplicationService = approvalApplicationService;
    }

    @GetMapping("/pending")
    @Operation(
            summary = "List pending approval requests",
            description = "Returns high-risk actions waiting for human review. Listing does not execute the action.")
    public ApiResponse<List<ApprovalRequestResponse>> pending() {
        return ApiResponse.success(approvalApplicationService.findPending().stream()
                .map(ApprovalRequestResponse::from)
                .toList());
    }

    @GetMapping("/{approvalRequestId}")
    @Operation(summary = "Get an approval request", description = "Reads one approval request without executing it.")
    public ApiResponse<ApprovalRequestResponse> getById(
            @Parameter(description = "Approval request id.", example = "APR-DEMO-1001")
            @PathVariable String approvalRequestId) {
        return ApiResponse.success(ApprovalRequestResponse.from(
                approvalApplicationService.getById(approvalRequestId)));
    }

    @PostMapping("/{approvalRequestId}/approve")
    @Operation(
            summary = "Approve a pending request",
            description = "Records a human approval decision. The platform remains explicit about high-risk actions "
                    + "and does not document automatic real refund, exchange, payment, or logistics execution.")
    public ApiResponse<ApprovalRequestResponse> approve(
            @Parameter(description = "Approval request id.", example = "APR-DEMO-1001")
            @PathVariable String approvalRequestId,
            @RequestBody ApprovalApproveRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return ApiResponse.success(ApprovalRequestResponse.from(approvalApplicationService.approve(
                approvalRequestId,
                request.reviewerId(),
                request.reason())));
    }

    @PostMapping("/{approvalRequestId}/reject")
    @Operation(summary = "Reject a pending request", description = "Records a human rejection decision.")
    public ApiResponse<ApprovalRequestResponse> reject(
            @Parameter(description = "Approval request id.", example = "APR-DEMO-1001")
            @PathVariable String approvalRequestId,
            @RequestBody ApprovalRejectRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return ApiResponse.success(ApprovalRequestResponse.from(approvalApplicationService.reject(
                approvalRequestId,
                request.reviewerId(),
                request.reason())));
    }
}
