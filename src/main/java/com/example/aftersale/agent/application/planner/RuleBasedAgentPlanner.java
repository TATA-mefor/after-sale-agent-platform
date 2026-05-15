package com.example.aftersale.agent.application.planner;

import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import java.util.List;
import java.util.Locale;

public class RuleBasedAgentPlanner implements AgentPlanner {

    @Override
    public AgentPlan plan(AgentPlanningContext context) {
        if (isMultiIntentMessage(context.rawUserMessage())) {
            return multiIntentPlan(context);
        }
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

    private static AgentPlan multiIntentPlan(AgentPlanningContext context) {
        return new AgentPlan(
                IntentType.MULTI_INTENT,
                ToolRiskLevel.MEDIUM,
                "服装退货 换货 尺码 优惠券 未使用",
                "用户一次提出退货、换货和优惠券咨询诉求，需按结构化子任务顺序处理。",
                "该售后问题包含 RETURN、EXCHANGE、COUPON_CONSULTATION 三个子任务，建议分别处理并记录依据。",
                List.of(
                        "User message: " + context.rawUserMessage(),
                        "Detected return, exchange, and coupon consultation in one ticket.",
                        "Execute subtasks sequentially through ToolRegistry."),
                defaultPlannedTools("Plan shared order, policy, and note tools for multi-intent handling."),
                List.of(
                        new AgentSubtask(
                                "subtask-1",
                                SubtaskType.RETURN,
                                "有退货诉求的商品",
                                "退货",
                                1,
                                ToolRiskLevel.MEDIUM,
                                "质量问题 退货 退货退款",
                                defaultPlannedTools("Handle RETURN subtask."),
                                List.of()),
                        new AgentSubtask(
                                "subtask-2",
                                SubtaskType.EXCHANGE,
                                "需要换尺码或换货的商品",
                                "换尺码/换货",
                                2,
                                ToolRiskLevel.MEDIUM,
                                "换货 尺码不合适",
                                defaultPlannedTools("Handle EXCHANGE subtask."),
                                List.of()),
                        new AgentSubtask(
                                "subtask-3",
                                SubtaskType.COUPON_CONSULTATION,
                                "未使用优惠券",
                                "优惠券没用上怎么退",
                                3,
                                ToolRiskLevel.LOW,
                                "优惠券 未使用 退还",
                                defaultPlannedTools("Handle COUPON_CONSULTATION subtask."),
                                List.of())));
    }

    private static List<PlannedToolCall> defaultPlannedTools(String noteReason) {
        return List.of(
                new PlannedToolCall(
                        "get_order_by_id",
                        "Retrieve order facts for the after-sale ticket."),
                new PlannedToolCall(
                        "search_aftersale_policy",
                        "Retrieve after-sale policy evidence."),
                new PlannedToolCall(
                        "add_ticket_note",
                        noteReason));
    }

    private static boolean isMultiIntentMessage(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        boolean hasReturn = normalized.contains("退货") || normalized.contains("退款");
        boolean hasExchange = normalized.contains("换尺码")
                || normalized.contains("换货")
                || normalized.contains("尺码");
        boolean hasCoupon = normalized.contains("优惠券");
        return hasReturn && hasExchange && hasCoupon;
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
