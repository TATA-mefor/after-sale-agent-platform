package com.example.aftersale.agent.application.handler;

import com.example.aftersale.agent.application.planner.SubtaskType;
import com.example.aftersale.tool.application.ToolRegistry;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class HumanEscalationHandler extends AbstractSpecialistAgentHandler {

    public HumanEscalationHandler(ToolRegistry toolRegistry) {
        super(toolRegistry);
    }

    @Override
    public SubtaskType supportedType() {
        return SubtaskType.HUMAN_ESCALATION;
    }

    @Override
    protected List<String> requiredToolNames() {
        return List.of(ADD_TICKET_NOTE_TOOL);
    }
}
