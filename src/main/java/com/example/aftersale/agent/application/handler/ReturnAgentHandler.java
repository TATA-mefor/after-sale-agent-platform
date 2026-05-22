package com.example.aftersale.agent.application.handler;

import com.example.aftersale.agent.application.planner.SubtaskType;
import com.example.aftersale.tool.application.ToolRegistry;
import java.util.List;
import org.springframework.stereotype.Component;
//据子任务语义组织执行步骤
@Component
public class ReturnAgentHandler extends AbstractSpecialistAgentHandler {

    public ReturnAgentHandler(ToolRegistry toolRegistry) {
        super(toolRegistry);
    }

    @Override
    public SubtaskType supportedType() {
        return SubtaskType.RETURN;
    }

    @Override
    protected List<String> requiredToolNames() {
        return List.of(GET_ORDER_BY_ID_TOOL, SEARCH_POLICY_TOOL, ADD_TICKET_NOTE_TOOL);
    }

    @Override
    protected String successRecommendation(SubtaskExecutionContext context) {
        return ItemRecommendationSupport.returnRecommendation(context);
    }
}
