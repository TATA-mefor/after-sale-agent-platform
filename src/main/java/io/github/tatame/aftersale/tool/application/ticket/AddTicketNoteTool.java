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

/**
 * 提供向工单写入内部备注的低风险工具。
 *
 * <p>边界：备注用于解释 Agent 处理过程和证据，不代表退款、补偿、换货等高风险动作已经完成。
 */
@Component
public class AddTicketNoteTool implements ToolExecutor {

    private static final ToolDefinition DEFINITION = ToolDefinition.of(
            "add_ticket_note",
            "Add an internal note to an after-sale ticket.",
            "{\"ticketId\":\"string\",\"note\":\"string\"}",
            "{\"ticketId\":\"string\",\"status\":\"string\",\"internalNote\":\"string\"}",
            ToolRiskLevel.LOW);

    private final TicketApplicationService ticketApplicationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the application service dependency.")
    public AddTicketNoteTool(TicketApplicationService ticketApplicationService) {
        this.ticketApplicationService = ticketApplicationService;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolOutput execute(ToolInput input) {
        Ticket ticket = ticketApplicationService.addTicketNote(
                input.requireString("ticketId"),
                input.requireString("note"));
        return ToolOutput.succeeded(DEFINITION.toolName(), Map.of(
                "ticketId", ticket.getTicketId(),
                "status", ticket.getStatus().name(),
                "internalNote", ticket.getInternalNote()));
    }
}
