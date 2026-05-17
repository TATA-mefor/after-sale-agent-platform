package com.example.aftersale.agent.application.planner;

import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RuleBasedAgentPlanner implements AgentPlanner {

    @Override
    public AgentPlan plan(AgentPlanningContext context) {
        String message = context.rawUserMessage();
        List<DetectedSubtask> detectedSubtasks = detectSubtasks(message);
        ToolRiskLevel planRiskLevel = riskLevel(message);
        if (!detectedSubtasks.isEmpty()) {
            return planWithSubtasks(context, detectedSubtasks, planRiskLevel);
        }
        IntentType intent = classifyIntent(message);
        return new AgentPlan(
                intent,
                planRiskLevel,
                policyQueryFor(intent, message),
                "Intent " + intent.name() + " identified. Agent suggestion should be based on policy evidence.",
                "Intent " + intent.name() + " identified. Suggested handling is based on policy evidence.",
                List.of(
                        "User message: " + message,
                        "Retrieve order facts for order " + context.orderId(),
                        "Retrieve after-sale policy evidence for intent " + intent.name(),
                        "Risk level assigned by deterministic rule fallback: " + planRiskLevel.name()),
                defaultPlannedTools("Persist the rule-based Agent suggestion on the ticket."));
    }

    private static AgentPlan planWithSubtasks(
            AgentPlanningContext context,
            List<DetectedSubtask> detectedSubtasks,
            ToolRiskLevel planRiskLevel) {
        ToolRiskLevel effectivePlanRiskLevel = detectedSubtasks.size() > 1 && planRiskLevel != ToolRiskLevel.HIGH
                ? ToolRiskLevel.MEDIUM
                : planRiskLevel;
        IntentType intent = detectedSubtasks.size() > 1
                ? IntentType.MULTI_INTENT
                : intentForSingleSubtask(detectedSubtasks.get(0).type());
        List<AgentSubtask> subtasks = toAgentSubtasks(detectedSubtasks, effectivePlanRiskLevel);
        return new AgentPlan(
                intent,
                effectivePlanRiskLevel,
                combinedPolicyQuery(subtasks),
                "用户诉求由规则型 fallback 拆解为结构化子任务，需按子任务顺序处理。",
                "该售后问题包含 " + subtaskSummary(subtasks) + " 子任务，建议分别处理并记录依据。",
                List.of(
                        "User message: " + context.rawUserMessage(),
                        "Detected structured subtasks: " + subtaskSummary(subtasks),
                        "Execute subtasks sequentially through ToolRegistry.",
                        "Risk level assigned by deterministic rule fallback: " + effectivePlanRiskLevel.name()),
                defaultPlannedTools("Plan shared order, policy, and note tools for subtask handling."),
                subtasks);
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

    private static List<DetectedSubtask> detectSubtasks(String message) {
        String normalized = normalize(message);
        List<DetectedSubtask> subtasks = new ArrayList<>();
        if (hasReturnIntent(normalized)) {
            subtasks.add(new DetectedSubtask(
                    SubtaskType.RETURN,
                    "有退货诉求的商品",
                    "退货/退货退款",
                    "质量问题 退货 退货退款",
                    ToolRiskLevel.MEDIUM));
        }
        if (hasExchangeIntent(normalized)) {
            subtasks.add(new DetectedSubtask(
                    SubtaskType.EXCHANGE,
                    "需要换尺码或换货的商品",
                    "换尺码/换货",
                    "换货 尺码不合适",
                    ToolRiskLevel.MEDIUM));
        }
        if (hasLogisticsIntent(normalized)) {
            subtasks.add(new DetectedSubtask(
                    SubtaskType.LOGISTICS_ISSUE,
                    "物流争议订单",
                    "物流/未收到货",
                    "物流 显示签收 未收到货",
                    ToolRiskLevel.LOW));
        }
        if (hasRefundOnlyIntent(normalized) && (subtasks.isEmpty() || hasLogisticsIntent(normalized))) {
            subtasks.add(new DetectedSubtask(
                    SubtaskType.REFUND_ONLY,
                    "仅退款诉求",
                    "仅退款/取消并退款",
                    "仅退款 退款 质量问题",
                    ToolRiskLevel.LOW));
        }
        if (hasCouponIntent(normalized)) {
            subtasks.add(new DetectedSubtask(
                    SubtaskType.COUPON_CONSULTATION,
                    "优惠券权益",
                    "优惠券咨询",
                    "优惠券 未使用 退券 补券",
                    ToolRiskLevel.LOW));
        }
        if (subtasks.size() > 1 || hasCouponIntent(normalized)) {
            return List.copyOf(subtasks);
        }
        return List.of();
    }

    private static IntentType classifyIntent(String message) {
        String normalized = normalize(message);
        if (hasLogisticsIntent(normalized)) {
            return IntentType.LOGISTICS_ISSUE;
        }
        if (hasRefundOnlyIntent(normalized)) {
            return IntentType.REFUND_ONLY;
        }
        if (hasExchangeIntent(normalized)) {
            return IntentType.EXCHANGE;
        }
        if (hasRepairIntent(normalized)) {
            return IntentType.REPAIR;
        }
        if (hasReturnIntent(normalized) || normalized.contains("退款")) {
            return IntentType.RETURN_AND_REFUND;
        }
        if (containsAny(normalized, "售后流程", "准备哪些材料", "需要准备", "怎么申请售后")) {
            return IntentType.GENERAL_CONSULTATION;
        }
        return IntentType.UNKNOWN;
    }

    private static List<AgentSubtask> toAgentSubtasks(
            List<DetectedSubtask> detectedSubtasks,
            ToolRiskLevel planRiskLevel) {
        List<AgentSubtask> subtasks = new ArrayList<>();
        for (int index = 0; index < detectedSubtasks.size(); index++) {
            DetectedSubtask detected = detectedSubtasks.get(index);
            ToolRiskLevel riskLevel = planRiskLevel == ToolRiskLevel.HIGH
                    ? ToolRiskLevel.HIGH
                    : detected.riskLevel();
            subtasks.add(new AgentSubtask(
                    "subtask-" + (index + 1),
                    detected.type(),
                    detected.target(),
                    detected.fragment(),
                    index + 1,
                    riskLevel,
                    detected.policyQuery(),
                    defaultPlannedTools("Handle " + detected.type().name() + " subtask."),
                    List.of()));
        }
        return List.copyOf(subtasks);
    }

    private static IntentType intentForSingleSubtask(SubtaskType type) {
        return switch (type) {
            case REFUND_ONLY -> IntentType.REFUND_ONLY;
            case RETURN -> IntentType.RETURN_AND_REFUND;
            case EXCHANGE -> IntentType.EXCHANGE;
            case REPAIR -> IntentType.REPAIR;
            case LOGISTICS_ISSUE -> IntentType.LOGISTICS_ISSUE;
            case COUPON_CONSULTATION, GENERAL_CONSULTATION -> IntentType.GENERAL_CONSULTATION;
            case HUMAN_ESCALATION, UNKNOWN -> IntentType.UNKNOWN;
        };
    }

    private static String combinedPolicyQuery(List<AgentSubtask> subtasks) {
        return String.join(" ", subtasks.stream().map(AgentSubtask::policyQuery).toList());
    }

    private static String subtaskSummary(List<AgentSubtask> subtasks) {
        return String.join(", ", subtasks.stream().map(subtask -> subtask.type().name()).toList());
    }

    private static ToolRiskLevel riskLevel(String message) {
        String normalized = normalize(message);
        if (containsAny(
                normalized,
                "直接退款",
                "立刻退款",
                "马上退款",
                "强制退款",
                "投诉",
                "平台介入",
                "金额较大",
                "金额较高",
                "多次售后",
                "关闭争议",
                "补偿",
                "赔偿")) {
            return ToolRiskLevel.HIGH;
        }
        return ToolRiskLevel.LOW;
    }

    private static String policyQueryFor(IntentType intent, String message) {
        String normalized = normalize(message);
        if (containsAny(normalized, "生鲜", "定制", "拆封", "不支持退货")) {
            return "特殊商品 生鲜 拆封 不支持";
        }
        if (containsAny(normalized, "过了售后期", "超过售后期", "售后期", "七天", "7天", "无理由")) {
            return "7天 无理由 退货 售后期";
        }
        return switch (intent) {
            case REFUND_ONLY -> "仅退款 退款 质量问题";
            case REPAIR -> "维修 保修期";
            case EXCHANGE -> "换货 尺码不合适";
            case LOGISTICS_ISSUE -> "物流 显示签收 未收到货";
            case RETURN_AND_REFUND -> "质量问题 退货 退货退款";
            case GENERAL_CONSULTATION -> "售后流程 材料";
            case MULTI_INTENT, UNKNOWN -> message;
        };
    }

    private static boolean hasReturnIntent(String normalized) {
        return containsAny(normalized, "退货", "退货退款", "无理由退", "生鲜商品拆封");
    }

    private static boolean hasRefundOnlyIntent(String normalized) {
        return containsAny(
                normalized,
                "仅退款",
                "只退款",
                "不退货退款",
                "没收到货想退款",
                "未收到货想退款",
                "没收到货，能不能退款",
                "没收到货能不能退款",
                "未发货取消并退款",
                "还没发货",
                "没有发货",
                "取消订单并退款",
                "取消并退款");
    }

    private static boolean hasCouponIntent(String normalized) {
        return containsAny(
                normalized,
                "优惠券",
                "券没用上",
                "优惠没退",
                "优惠券怎么退",
                "退券",
                "补券");
    }

    private static boolean hasExchangeIntent(String normalized) {
        return containsAny(normalized, "换尺码", "换货", "换大", "换小", "尺码");
    }

    private static boolean hasRepairIntent(String normalized) {
        return containsAny(normalized, "维修", "修理", "保修");
    }

    private static boolean hasLogisticsIntent(String normalized) {
        return containsAny(normalized, "物流", "没收到", "未收到", "签收", "没有收到货");
    }

    private static boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String message) {
        return message.toLowerCase(Locale.ROOT).replace(" ", "");
    }

    private record DetectedSubtask(
            SubtaskType type,
            String target,
            String fragment,
            String policyQuery,
            ToolRiskLevel riskLevel) {
    }
}
