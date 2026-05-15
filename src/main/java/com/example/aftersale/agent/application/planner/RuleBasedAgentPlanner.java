package com.example.aftersale.agent.application.planner;

import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import java.util.List;
import java.util.Locale;

public class RuleBasedAgentPlanner implements AgentPlanner {

    @Override
    public AgentPlan plan(AgentPlanningContext context) {
        IntentType intent = classifyIntent(context.rawUserMessage());
        return new AgentPlan(
                intent,
                ToolRiskLevel.LOW,
                context.rawUserMessage(),
                "Intent " + intent.name() + " identified. Agent suggestion should be based on policy evidence.",
                "Intent " + intent.name() + " identified. Suggested handling is based on policy evidence.",
                List.of(
                        "User message: " + context.rawUserMessage(),
                        "Retrieve order facts for order " + context.orderId(),
                        "Retrieve after-sale policy evidence for intent " + intent.name()),
                List.of(
                        new PlannedToolCall(
                                "get_order_by_id",
                                "Retrieve order facts for the after-sale ticket."),
                        new PlannedToolCall(
                                "search_aftersale_policy",
                                "Retrieve after-sale policy evidence."),
                        new PlannedToolCall(
                                "add_ticket_note",
                                "Persist the rule-based Agent suggestion on the ticket.")));
    }

    private static IntentType classifyIntent(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("物流") || normalized.contains("没收到") || normalized.contains("未收到")) {
            return IntentType.LOGISTICS_ISSUE;
        }
        if (normalized.contains("换货") || normalized.contains("换大") || normalized.contains("尺码")) {
            return IntentType.EXCHANGE;
        }
        if (normalized.contains("维修") || normalized.contains("修")) {
            return IntentType.REPAIR;
        }
        if (normalized.contains("退货") || normalized.contains("退款")) {
            return IntentType.RETURN_AND_REFUND;
        }
        return IntentType.UNKNOWN;
    }
}
