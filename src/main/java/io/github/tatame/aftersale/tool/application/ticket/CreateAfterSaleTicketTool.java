package io.github.tatame.aftersale.tool.application.ticket;

import io.github.tatame.aftersale.ticket.application.TicketApplicationService;
import io.github.tatame.aftersale.ticket.domain.Ticket;
import io.github.tatame.aftersale.tool.application.ToolExecutor;
import io.github.tatame.aftersale.tool.domain.ToolDefinition;
import io.github.tatame.aftersale.tool.domain.ToolInput;
import io.github.tatame.aftersale.tool.domain.ToolOutput;
import io.github.tatame.aftersale.tool.domain.ToolRiskLevel;
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
