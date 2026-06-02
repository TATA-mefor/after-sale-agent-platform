package io.github.tatame.aftersale.agent.application.handler;

import io.github.tatame.aftersale.agent.application.planner.SubtaskType;
import io.github.tatame.aftersale.tool.application.ToolRegistry;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LogisticsAgentHandler extends AbstractSpecialistAgentHandler {

    public LogisticsAgentHandler(ToolRegistry toolRegistry) {
        super(toolRegistry);
    }

    @Override
    public SubtaskType supportedType() {
        return SubtaskType.LOGISTICS_ISSUE;
    }

    @Override
    protected List<String> requiredToolNames() {
        return List.of(GET_ORDER_BY_ID_TOOL, SEARCH_POLICY_TOOL, ADD_TICKET_NOTE_TOOL);
    }
}
