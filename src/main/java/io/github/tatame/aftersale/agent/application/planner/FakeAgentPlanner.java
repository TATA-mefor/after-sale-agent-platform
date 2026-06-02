package io.github.tatame.aftersale.agent.application.planner;

import io.github.tatame.aftersale.ticket.domain.IntentType;
import io.github.tatame.aftersale.tool.domain.ToolRiskLevel;
import java.util.List;

public class FakeAgentPlanner implements AgentPlanner {

    @Override
    public AgentPlan plan(AgentPlanningContext context) {
        return new AgentPlan(
                IntentType.RETURN_AND_REFUND,
                ToolRiskLevel.LOW,
                "质量问题 退货 退款",
                "Fake planner note for RETURN_AND_REFUND.",
                "Fake planner final suggestion for RETURN_AND_REFUND.",
                List.of("Fake planner evidence hint."),
                List.of(
                        new PlannedToolCall("search_aftersale_policy", "Fake policy search."),
                        new PlannedToolCall("add_ticket_note", "Fake note persistence.")));
    }
}
