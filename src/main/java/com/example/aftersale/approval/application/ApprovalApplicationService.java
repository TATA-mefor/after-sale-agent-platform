package com.example.aftersale.approval.application;

import com.example.aftersale.approval.domain.ApprovalRepository;
import com.example.aftersale.approval.domain.ApprovalRequest;
import com.example.aftersale.approval.domain.ApprovalStatus;
import com.example.aftersale.common.exception.ResourceNotFoundException;
import com.example.aftersale.common.observability.MdcScope;
import com.example.aftersale.common.observability.ObservabilityConstants;
import com.example.aftersale.ticket.application.TicketApplicationService;
import com.example.aftersale.ticket.domain.TicketStatus;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ApprovalApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApprovalApplicationService.class);

    private static final String SPECIALIST_SUBTASK_ACTION = "specialist_subtask";

    private final ApprovalRepository approvalRepository;
    private final TicketApplicationService ticketApplicationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores application collaborators.")
    public ApprovalApplicationService(
            ApprovalRepository approvalRepository,
            TicketApplicationService ticketApplicationService) {
        this.approvalRepository = approvalRepository;
        this.ticketApplicationService = ticketApplicationService;
    }

    public ApprovalRequest createForHighRiskSubtask(
            String ticketId,
            String runId,
            String subtaskId,
            String requestedAction,
            ToolRiskLevel riskLevel) {
        ApprovalRequest request = ApprovalRequest.createForHighRiskTool(
                "APP-" + UUID.randomUUID(),
                ticketId,
                runId,
                subtaskId,
                SPECIALIST_SUBTASK_ACTION,
                requestedAction,
                riskLevel,
                Instant.now());
        ApprovalRequest saved = approvalRepository.save(request);
        logApprovalCreated(saved);
        ticketApplicationService.addTicketNote(ticketId, "Approval request created: " + saved.getApprovalId()
                + " for " + requestedAction);
        ticketApplicationService.updateTicketStatus(ticketId, TicketStatus.WAITING_HUMAN_APPROVAL, null);
        return saved;
    }

    public ApprovalRequest createForHighRiskTool(
            String ticketId,
            String runId,
            String subtaskId,
            String toolName,
            String requestedAction,
            ToolRiskLevel riskLevel) {
        ApprovalRequest request = ApprovalRequest.createForHighRiskTool(
                "APP-" + UUID.randomUUID(),
                ticketId,
                runId,
                subtaskId,
                toolName,
                requestedAction,
                riskLevel,
                Instant.now());
        ApprovalRequest saved = approvalRepository.save(request);
        logApprovalCreated(saved);
        return saved;
    }

    public List<ApprovalRequest> findPending() {
        return approvalRepository.findByStatus(ApprovalStatus.PENDING);
    }

    public List<ApprovalRequest> findByRunId(String runId) {
        return approvalRepository.findByRunId(requireText(runId, "runId"));
    }

    public ApprovalRequest getById(String approvalRequestId) {
        return approvalRepository.findById(requireText(approvalRequestId, "approvalRequestId"))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "APPROVAL_REQUEST_NOT_FOUND",
                        "ApprovalRequest not found: " + approvalRequestId));
    }

    public ApprovalRequest approve(String approvalRequestId, String reviewerId, String reason) {
        ApprovalRequest request = getById(approvalRequestId);
        request.approve(reviewerId, reason, Instant.now());
        ApprovalRequest saved = approvalRepository.save(request);
        logApprovalDecision(saved, "approval.approved");
        ticketApplicationService.addTicketNote(saved.getTicketId(), "Approval approved: " + saved.getApprovalId()
                + ". Reviewer=" + saved.getReviewerId() + ". Reason=" + saved.getDecisionReason());
        ticketApplicationService.updateTicketStatus(saved.getTicketId(), TicketStatus.PROCESSING, null);
        return saved;
    }

    public ApprovalRequest reject(String approvalRequestId, String reviewerId, String reason) {
        ApprovalRequest request = getById(approvalRequestId);
        request.reject(reviewerId, reason, Instant.now());
        ApprovalRequest saved = approvalRepository.save(request);
        logApprovalDecision(saved, "approval.rejected");
        ticketApplicationService.addTicketNote(saved.getTicketId(), "Approval rejected: " + saved.getApprovalId()
                + ". Reviewer=" + saved.getReviewerId() + ". Reason=" + saved.getDecisionReason());
        ticketApplicationService.updateTicketStatus(
                saved.getTicketId(),
                TicketStatus.REJECTED,
                "Approval rejected: " + saved.getDecisionReason());
        return saved;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static void logApprovalCreated(ApprovalRequest request) {
        try (MdcScope ignored = MdcScope.putAll(approvalMdcValues(request))) {
            LOGGER.info("approval.created toolName={} riskLevel={} status={}",
                    request.getToolName(),
                    request.getRiskLevel(),
                    request.getStatus());
        }
    }

    private static void logApprovalDecision(ApprovalRequest request, String eventName) {
        try (MdcScope ignored = MdcScope.putAll(approvalMdcValues(request))) {
            LOGGER.info("{} status={} reviewerId={}", eventName, request.getStatus(), request.getReviewerId());
        }
    }

    private static Map<String, Object> approvalMdcValues(ApprovalRequest request) {
        return Map.of(
                ObservabilityConstants.APPROVAL_REQUEST_ID, request.getApprovalId(),
                ObservabilityConstants.TICKET_ID, request.getTicketId(),
                ObservabilityConstants.AGENT_RUN_ID, request.getRunId(),
                ObservabilityConstants.SUBTASK_ID, request.getSubtaskId());
    }
}
