package io.github.tatame.aftersale.agent.application.handler;

import io.github.tatame.aftersale.agent.application.planner.SubtaskType;
import io.github.tatame.aftersale.tool.application.ToolRegistry;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CouponAgentHandler extends AbstractSpecialistAgentHandler {

    public CouponAgentHandler(ToolRegistry toolRegistry) {
        super(toolRegistry);
    }

    @Override
    public SubtaskType supportedType() {
        return SubtaskType.COUPON_CONSULTATION;
    }

    @Override
    protected List<String> requiredToolNames() {
        return List.of(SEARCH_POLICY_TOOL, ADD_TICKET_NOTE_TOOL);
    }
}
