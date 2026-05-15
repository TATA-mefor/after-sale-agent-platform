package com.example.aftersale.agent.application.handler;

import com.example.aftersale.agent.application.planner.SubtaskType;
import com.example.aftersale.tool.application.ToolRegistry;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GeneralConsultationHandler extends AbstractSpecialistAgentHandler {

    public GeneralConsultationHandler(ToolRegistry toolRegistry) {
        super(toolRegistry);
    }

    @Override
    public SubtaskType supportedType() {
        return SubtaskType.GENERAL_CONSULTATION;
    }

    @Override
    protected List<String> requiredToolNames() {
        return List.of(SEARCH_POLICY_TOOL, ADD_TICKET_NOTE_TOOL);
    }
}
