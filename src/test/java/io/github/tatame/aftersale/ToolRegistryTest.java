package io.github.tatame.aftersale;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.tatame.aftersale.common.observability.metrics.ApplicationMetricsRecorder;
import io.github.tatame.aftersale.ticket.application.TicketApplicationService;
import io.github.tatame.aftersale.ticket.domain.Ticket;
import io.github.tatame.aftersale.ticket.domain.TicketStatus;
import io.github.tatame.aftersale.tool.application.ToolExecutor;
import io.github.tatame.aftersale.tool.application.ToolRegistry;
import io.github.tatame.aftersale.tool.domain.ToolDefinition;
import io.github.tatame.aftersale.tool.domain.ToolExecutionStatus;
import io.github.tatame.aftersale.tool.domain.ToolInput;
import io.github.tatame.aftersale.tool.domain.ToolOutput;
import io.github.tatame.aftersale.tool.domain.ToolRiskLevel;
import java.util.List;
import java.util.Map;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ToolRegistryTest {

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private TicketApplicationService ticketApplicationService;

    @Test
    void canFindToolByName() {
        assertThat(toolRegistry.findDefinition("create_aftersale_ticket"))
                .hasValueSatisfying(definition -> {
                    assertThat(definition.toolName()).isEqualTo("create_aftersale_ticket");
                    assertThat(definition.inputSchema()).contains("userId");
                    assertThat(definition.outputSchema()).contains("ticketId");
                    assertThat(definition.riskLevel()).isEqualTo(ToolRiskLevel.LOW);
                    assertThat(definition.requiresApproval()).isFalse();
                });
    }

    @Test
    void unknownToolReturnsFailure() {
        ToolOutput output = toolRegistry.execute("missing_tool", ToolInput.empty());

        assertThat(output.status()).isEqualTo(ToolExecutionStatus.FAILED);
        assertThat(output.errorCode()).isEqualTo("TOOL_NOT_FOUND");
        assertThat(output.message()).contains("missing_tool");
    }

    @Test
    void lowRiskCreateTicketToolExecutesThroughApplicationService() {
        ToolOutput output = toolRegistry.execute("create_aftersale_ticket", ToolInput.of(Map.of(
                "userId", "U-5001",
                "orderId", "O-5001",
                "message", "The item arrived damaged.")));

        assertThat(output.status()).isEqualTo(ToolExecutionStatus.SUCCEEDED);
        assertThat(output.data()).containsEntry("status", TicketStatus.CREATED.name());

        String ticketId = (String) output.data().get("ticketId");
        Ticket ticket = ticketApplicationService.getTicket(ticketId);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CREATED);
    }

    @Test
    void addTicketNoteToolExecutesAndStoresInternalNote() {
        Ticket ticket = ticketApplicationService.createTicket("U-5002", "O-5002", "I need after-sale help.");

        ToolOutput output = toolRegistry.execute("add_ticket_note", ToolInput.of(Map.of(
                "ticketId", ticket.getTicketId(),
                "note", "Customer asked for manual review.")));

        assertThat(output.status()).isEqualTo(ToolExecutionStatus.SUCCEEDED);
        assertThat(output.data()).containsEntry("internalNote", "Customer asked for manual review.");
        assertThat(ticketApplicationService.getTicket(ticket.getTicketId()).getInternalNote())
                .isEqualTo("Customer asked for manual review.");
    }

    @Test
    void highRiskToolRequiresApprovalBeforeExecutorRuns() {
        HighRiskTestTool executor = new HighRiskTestTool();
        ToolRegistry registry = new ToolRegistry(List.of(executor), record -> {
        }, testMetricsRecorder());

        ToolOutput output = registry.execute("issue_refund", ToolInput.empty());

        assertThat(output.status()).isEqualTo(ToolExecutionStatus.REQUIRES_APPROVAL);
        assertThat(output.message()).contains("human approval");
        assertThat(executor.executed).isFalse();
    }

    private static ApplicationMetricsRecorder testMetricsRecorder() {
        return new ApplicationMetricsRecorder(new SimpleMeterRegistry());
    }

    @Test
    void highRiskTicketCloseActionRequiresApproval() {
        Ticket ticket = ticketApplicationService.createTicket("U-5003", "O-5003", "Close this after-sale request.");

        ToolOutput output = toolRegistry.execute("update_ticket_status", ToolInput.of(Map.of(
                "ticketId", ticket.getTicketId(),
                "status", TicketStatus.CLOSED.name(),
                "reason", "Close requested.")));

        assertThat(output.status()).isEqualTo(ToolExecutionStatus.REQUIRES_APPROVAL);
        assertThat(ticketApplicationService.getTicket(ticket.getTicketId()).getStatus())
                .isEqualTo(TicketStatus.CREATED);
    }

    @Test
    void toolExecutionFailureIncludesErrorInformation() {
        ToolOutput output = toolRegistry.execute("add_ticket_note", ToolInput.of(Map.of(
                "ticketId", "T-MISSING-M5",
                "note", "This should fail.")));

        assertThat(output.status()).isEqualTo(ToolExecutionStatus.FAILED);
        assertThat(output.errorCode()).isEqualTo("TOOL_EXECUTION_FAILED");
        assertThat(output.message()).contains("T-MISSING-M5");
    }

    private static final class HighRiskTestTool implements ToolExecutor {

        private boolean executed;

        @Override
        public ToolDefinition definition() {
            return ToolDefinition.of(
                    "issue_refund",
                    "Request a refund for a paid order.",
                    "{\"ticketId\":\"string\",\"amount\":\"number\"}",
                    "{\"approvalId\":\"string\"}",
                    ToolRiskLevel.HIGH);
        }

        @Override
        public ToolOutput execute(ToolInput input) {
            executed = true;
            return ToolOutput.succeeded("issue_refund", Map.of("executed", true));
        }
    }
}
