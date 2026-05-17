package com.example.aftersale.approval.api;

import com.example.aftersale.approval.application.ApprovalApplicationService;
import com.example.aftersale.common.api.ApiResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
public class ApprovalController {

    private final ApprovalApplicationService approvalApplicationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the application service dependency.")
    public ApprovalController(ApprovalApplicationService approvalApplicationService) {
        this.approvalApplicationService = approvalApplicationService;
    }

    @GetMapping("/pending")
    public ApiResponse<List<ApprovalRequestResponse>> pending() {
        return ApiResponse.success(approvalApplicationService.findPending().stream()
                .map(ApprovalRequestResponse::from)
                .toList());
    }

    @GetMapping("/{approvalRequestId}")
    public ApiResponse<ApprovalRequestResponse> getById(@PathVariable String approvalRequestId) {
        return ApiResponse.success(ApprovalRequestResponse.from(
                approvalApplicationService.getById(approvalRequestId)));
    }

    @PostMapping("/{approvalRequestId}/approve")
    public ApiResponse<ApprovalRequestResponse> approve(
            @PathVariable String approvalRequestId,
            @RequestBody ApprovalApproveRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return ApiResponse.success(ApprovalRequestResponse.from(approvalApplicationService.approve(
                approvalRequestId,
                request.reviewerId(),
                request.reason())));
    }

    @PostMapping("/{approvalRequestId}/reject")
    public ApiResponse<ApprovalRequestResponse> reject(
            @PathVariable String approvalRequestId,
            @RequestBody ApprovalRejectRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return ApiResponse.success(ApprovalRequestResponse.from(approvalApplicationService.reject(
                approvalRequestId,
                request.reviewerId(),
                request.reason())));
    }
}
