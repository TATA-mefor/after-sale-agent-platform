package io.github.tatame.aftersale.tool.application.ticket;

import io.github.tatame.aftersale.ticket.application.TicketApplicationService;
import io.github.tatame.aftersale.ticket.domain.Ticket;
import io.github.tatame.aftersale.ticket.domain.TicketStatus;
import io.github.tatame.aftersale.tool.application.ToolExecutor;
import io.github.tatame.aftersale.tool.domain.ToolDefinition;
import io.github.tatame.aftersale.tool.domain.ToolInput;
import io.github.tatame.aftersale.tool.domain.ToolOutput;
import io.github.tatame.aftersale.tool.domain.ToolRiskLevel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class UpdateTicketStatusTool implements ToolExecutor {

    private static final ToolDefinition DEFINITION = ToolDefinition.of(
            "update_ticket_status",
            "Update a ticket status when the transition is allowed by the ticket domain model.",
            "{\"ticketId\":\"string\",\"status\":\"string\",\"reason\":\"string\"}",
            "{\"ticketId\":\"string\",\"status\":\"string\"}",
            ToolRiskLevel.MEDIUM);

    private final TicketApplicationService ticketApplicationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the application service dependency.")
    public UpdateTicketStatusTool(TicketApplicationService ticketApplicationService) {
        this.ticketApplicationService = ticketApplicationService;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolOutput execute(ToolInput input) {
        TicketStatus targetStatus = TicketStatus.valueOf(input.requireString("status"));
        if (targetStatus == TicketStatus.CLOSED) {
            return ToolOutput.requiresApproval(DEFINITION.toolName(), "Closing a ticket requires human approval.");
        }

        Ticket ticket = ticketApplicationService.updateTicketStatus(
                input.requireString("ticketId"),
                targetStatus,
                input.optionalString("reason").orElse(null));
        return ToolOutput.succeeded(DEFINITION.toolName(), Map.of(
                "ticketId", ticket.getTicketId(),
                "status", ticket.getStatus().name()));
    }
}
