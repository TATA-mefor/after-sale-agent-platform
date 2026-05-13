package com.example.aftersale.tool.application.ticket;

import com.example.aftersale.ticket.application.TicketApplicationService;
import com.example.aftersale.ticket.domain.Ticket;
import com.example.aftersale.tool.application.ToolExecutor;
import com.example.aftersale.tool.domain.ToolDefinition;
import com.example.aftersale.tool.domain.ToolInput;
import com.example.aftersale.tool.domain.ToolOutput;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CreateAfterSaleTicketTool implements ToolExecutor {

    private static final ToolDefinition DEFINITION = ToolDefinition.of(
            "create_aftersale_ticket",
            "Create an after-sale ticket from a user message.",
            "{\"userId\":\"string\",\"orderId\":\"string\",\"message\":\"string\"}",
            "{\"ticketId\":\"string\",\"status\":\"string\"}",
            ToolRiskLevel.LOW);

    private final TicketApplicationService ticketApplicationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the application service dependency.")
    public CreateAfterSaleTicketTool(TicketApplicationService ticketApplicationService) {
        this.ticketApplicationService = ticketApplicationService;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolOutput execute(ToolInput input) {
        Ticket ticket = ticketApplicationService.createTicket(
                input.requireString("userId"),
                input.requireString("orderId"),
                input.requireString("message"));
        return ToolOutput.succeeded(DEFINITION.toolName(), Map.of(
                "ticketId", ticket.getTicketId(),
                "status", ticket.getStatus().name()));
    }
}
