package io.github.tatame.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.tatame.aftersale.agent.domain.AgentRun;
import io.github.tatame.aftersale.agent.domain.AgentRunStatus;
import io.github.tatame.aftersale.approval.domain.ApprovalRequest;
import io.github.tatame.aftersale.approval.domain.ApprovalStatus;
import io.github.tatame.aftersale.ticket.domain.IntentType;
import io.github.tatame.aftersale.ticket.domain.Ticket;
import io.github.tatame.aftersale.ticket.domain.TicketStatus;
import io.github.tatame.aftersale.tool.domain.ToolRiskLevel;
import io.github.tatame.aftersale.trace.domain.ToolCallStatus;
import io.github.tatame.aftersale.trace.domain.ToolCallTrace;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CoreDomainModelTest {

    private static final Instant STARTED_AT = Instant.parse("2026-05-13T08:00:00Z");
    private static final Instant LATER_AT = Instant.parse("2026-05-13T08:05:00Z");

    @Test
    void ticketCreationUsesDefaultStatusAndUnknownIntent() {
        Ticket ticket = Ticket.create("T-1001", "U-1001", "O-1001", "left earbud is silent", STARTED_AT);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CREATED);
        assertThat(ticket.getIntentType()).isEqualTo(IntentType.UNKNOWN);
        assertThat(ticket.getPriority()).isEqualTo("NORMAL");
        assertThat(ticket.getCreatedAt()).isEqualTo(STARTED_AT);
        assertThat(ticket.getUpdatedAt()).isEqualTo(STARTED_AT);
    }

    @Test
    void ticketStatusTransitionsAreEncapsulatedByDomainMethods() {
        Ticket ticket = Ticket.create("T-1002", "U-1001", "O-1001", "left earbud is silent", STARTED_AT);

        ticket.classifyIntent(IntentType.RETURN_AND_REFUND, LATER_AT);
        ticket.startAgentRun(LATER_AT);
        ticket.waitForHumanApproval(LATER_AT);
        ticket.startProcessing(LATER_AT);
        ticket.resolve("Quality issue evidence is required before refund approval.", LATER_AT);
        ticket.close(LATER_AT);

        assertThat(ticket.getIntentType()).isEqualTo(IntentType.RETURN_AND_REFUND);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(ticket.getAgentSuggestion()).contains("Quality issue evidence");
    }

    @Test
    void agentRunRecordsSucceededAndFailedStates() {
        AgentRun succeededRun = AgentRun.start("R-1001", "T-1001", STARTED_AT);
        AgentRun failedRun = AgentRun.start("R-1002", "T-1001", STARTED_AT);

        succeededRun.succeed("{\"intent\":\"RETURN_AND_REFUND\"}", "Proceed to manual review.", LATER_AT);
        failedRun.fail("order lookup failed", LATER_AT);

        assertThat(succeededRun.getStatus()).isEqualTo(AgentRunStatus.SUCCEEDED);
        assertThat(succeededRun.getPlanJson()).contains("RETURN_AND_REFUND");
        assertThat(succeededRun.getFinalAnswer()).isEqualTo("Proceed to manual review.");
        assertThat(failedRun.getStatus()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(failedRun.getErrorMessage()).isEqualTo("order lookup failed");
    }

    @Test
    void toolCallTraceRecordsSucceededAndFailedCalls() {
        ToolCallTrace successTrace = ToolCallTrace.start(
                "TRACE-1001", "R-1001", "get_order_by_id", "{\"orderId\":\"O-1001\"}", STARTED_AT);
        ToolCallTrace failedTrace = ToolCallTrace.start(
                "TRACE-1002", "R-1001", "search_aftersale_policy", "{\"query\":\"refund\"}", STARTED_AT);

        successTrace.markSucceeded("{\"orderStatus\":\"DELIVERED\"}", 42);
        failedTrace.markFailed("policy index unavailable", 18);

        assertThat(successTrace.getStatus()).isEqualTo(ToolCallStatus.SUCCEEDED);
        assertThat(successTrace.getOutputJson()).contains("DELIVERED");
        assertThat(successTrace.getLatencyMs()).isEqualTo(42);
        assertThat(failedTrace.getStatus()).isEqualTo(ToolCallStatus.FAILED);
        assertThat(failedTrace.getErrorMessage()).isEqualTo("policy index unavailable");
    }

    @Test
    void highRiskToolRequiresApproval() {
        ApprovalRequest request = ApprovalRequest.createForHighRiskTool(
                "APP-1001",
                "T-1001",
                "R-1001",
                "issue_refund",
                "Refund paid amount to customer",
                ToolRiskLevel.HIGH,
                STARTED_AT);

        assertThat(ToolRiskLevel.HIGH.requiresApproval()).isTrue();
        assertThat(ToolRiskLevel.LOW.requiresApproval()).isFalse();
        assertThat(request.requiresApproval()).isTrue();
        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.PENDING);

        assertThatThrownBy(() -> ApprovalRequest.createForHighRiskTool(
                "APP-1002",
                "T-1001",
                "R-1001",
                "add_ticket_note",
                "Add internal note",
                ToolRiskLevel.LOW,
                STARTED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("approval-required");
    }
}
