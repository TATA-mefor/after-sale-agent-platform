package io.github.tatame.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.tatame.aftersale.approval.application.ApprovalApplicationService;
import io.github.tatame.aftersale.approval.domain.ApprovalRequest;
import io.github.tatame.aftersale.approval.domain.ApprovalStatus;
import io.github.tatame.aftersale.ticket.application.TicketApplicationService;
import io.github.tatame.aftersale.ticket.domain.Ticket;
import io.github.tatame.aftersale.ticket.domain.TicketStatus;
import io.github.tatame.aftersale.tool.domain.ToolRiskLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ApprovalApplicationServiceTest {

    @Autowired
    private ApprovalApplicationService approvalApplicationService;

    @Autowired
    private TicketApplicationService ticketApplicationService;

    @Test
    void canCreateAndQueryPendingApprovalRequest() {
        Ticket ticket = ticketApplicationService.createTicket(
                "U-APPROVAL-1",
                "O202605130001",
                "High risk refund requires review.");

        ApprovalRequest request = approvalApplicationService.createForHighRiskSubtask(
                ticket.getTicketId(),
                "RUN-APPROVAL-1",
                "subtask-high-risk-1",
                "Manual review before refund recommendation.",
                ToolRiskLevel.HIGH);

        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(request.getTicketId()).isEqualTo(ticket.getTicketId());
        assertThat(request.getRunId()).isEqualTo("RUN-APPROVAL-1");
        assertThat(request.getSubtaskId()).isEqualTo("subtask-high-risk-1");
        assertThat(approvalApplicationService.findPending())
                .extracting(ApprovalRequest::getApprovalId)
                .contains(request.getApprovalId());
        assertThat(approvalApplicationService.getById(request.getApprovalId()).getApprovalId())
                .isEqualTo(request.getApprovalId());
        assertThat(ticketApplicationService.getTicket(ticket.getTicketId()).getStatus())
                .isEqualTo(TicketStatus.WAITING_HUMAN_APPROVAL);
    }

    @Test
    void approveMovesRequestToApprovedAndWritesTicketNote() {
        Ticket ticket = ticketApplicationService.createTicket(
                "U-APPROVAL-2",
                "O202605130001",
                "High risk exchange requires review.");
        ApprovalRequest request = approvalApplicationService.createForHighRiskSubtask(
                ticket.getTicketId(),
                "RUN-APPROVAL-2",
                "subtask-high-risk-2",
                "Manual review before exchange recommendation.",
                ToolRiskLevel.HIGH);

        ApprovalRequest approved = approvalApplicationService.approve(
                request.getApprovalId(),
                "reviewer-1",
                "Evidence is sufficient.");

        assertThat(approved.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(approved.getReviewerId()).isEqualTo("reviewer-1");
        assertThat(approved.getDecisionReason()).isEqualTo("Evidence is sufficient.");
        Ticket updatedTicket = ticketApplicationService.getTicket(ticket.getTicketId());
        assertThat(updatedTicket.getStatus()).isEqualTo(TicketStatus.PROCESSING);
        assertThat(updatedTicket.getInternalNote()).contains("Approval approved", approved.getApprovalId());
    }

    @Test
    void rejectMovesRequestToRejectedAndRequiresReason() {
        Ticket ticket = ticketApplicationService.createTicket(
                "U-APPROVAL-3",
                "O202605130001",
                "High risk coupon compensation requires review.");
        ApprovalRequest request = approvalApplicationService.createForHighRiskSubtask(
                ticket.getTicketId(),
                "RUN-APPROVAL-3",
                "subtask-high-risk-3",
                "Manual review before coupon compensation.",
                ToolRiskLevel.HIGH);

        ApprovalRequest rejected = approvalApplicationService.reject(
                request.getApprovalId(),
                "reviewer-2",
                "Compensation policy does not apply.");

        assertThat(rejected.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(rejected.getDecisionReason()).isEqualTo("Compensation policy does not apply.");
        Ticket updatedTicket = ticketApplicationService.getTicket(ticket.getTicketId());
        assertThat(updatedTicket.getStatus()).isEqualTo(TicketStatus.REJECTED);
        assertThat(updatedTicket.getInternalNote()).contains("Approval rejected", rejected.getApprovalId());
        assertThat(updatedTicket.getAgentSuggestion()).contains("Compensation policy does not apply.");
    }

    @Test
    void completedApprovalCannotBeApprovedAgain() {
        Ticket ticket = ticketApplicationService.createTicket(
                "U-APPROVAL-4",
                "O202605130001",
                "High risk request.");
        ApprovalRequest request = approvalApplicationService.createForHighRiskSubtask(
                ticket.getTicketId(),
                "RUN-APPROVAL-4",
                "subtask-high-risk-4",
                "Manual review required.",
                ToolRiskLevel.HIGH);
        approvalApplicationService.approve(request.getApprovalId(), "reviewer-1", "Approved once.");

        assertThatThrownBy(() -> approvalApplicationService.approve(
                request.getApprovalId(),
                "reviewer-1",
                "Approved twice."))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not pending");
        assertThatThrownBy(() -> approvalApplicationService.reject(
                request.getApprovalId(),
                "reviewer-1",
                "Reject after approve."))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not pending");
    }

    @Test
    void lowRiskActionCannotCreateApprovalRequest() {
        Ticket ticket = ticketApplicationService.createTicket(
                "U-APPROVAL-5",
                "O202605130001",
                "Low risk request.");

        assertThatThrownBy(() -> approvalApplicationService.createForHighRiskTool(
                ticket.getTicketId(),
                "RUN-APPROVAL-5",
                "",
                "add_ticket_note",
                "Low risk note.",
                ToolRiskLevel.LOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("approval-required");
    }
}
