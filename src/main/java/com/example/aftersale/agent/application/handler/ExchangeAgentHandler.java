package com.example.aftersale.agent.application.handler;

import com.example.aftersale.agent.application.planner.SubtaskType;
import com.example.aftersale.tool.application.ToolRegistry;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ExchangeAgentHandler extends AbstractSpecialistAgentHandler {

    public ExchangeAgentHandler(ToolRegistry toolRegistry) {
        super(toolRegistry);
    }

    @Override
    public SubtaskType supportedType() {
        return SubtaskType.EXCHANGE;
    }

    @Override
    protected List<String> requiredToolNames() {
        return List.of(GET_ORDER_BY_ID_TOOL, SEARCH_POLICY_TOOL, ADD_TICKET_NOTE_TOOL);
    }

    @Override
    protected String successRecommendation(SubtaskExecutionContext context) {
        return ItemRecommendationSupport.exchangeRecommendation(context);
    }
}
