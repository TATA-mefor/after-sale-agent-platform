package io.github.tatame.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.tatame.aftersale.agent.application.planner.AgentPlan;
import io.github.tatame.aftersale.agent.application.planner.AgentPlanValidationException;
import io.github.tatame.aftersale.agent.application.planner.AgentPlanValidator;
import io.github.tatame.aftersale.agent.application.planner.AgentSubtask;
import io.github.tatame.aftersale.agent.application.planner.PlannedToolCall;
import io.github.tatame.aftersale.agent.application.planner.SubtaskType;
import io.github.tatame.aftersale.agent.infrastructure.llm.AgentPlanParser;
import io.github.tatame.aftersale.ticket.domain.IntentType;
import io.github.tatame.aftersale.tool.domain.ToolRiskLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentPlanParserTest {

    private final AgentPlanParser parser = new AgentPlanParser(new ObjectMapper());

    @Test
    void parsesValidAgentPlanJson() {
        AgentPlan plan = parser.parse(validPlanJson());

        assertThat(plan.intent()).isEqualTo(IntentType.RETURN_AND_REFUND);
        assertThat(plan.policyQuery()).isEqualTo("质量问题 退货 退款");
        assertThat(plan.plannedTools())
                .extracting("toolName")
                .containsExactly("search_aftersale_policy", "add_ticket_note");
    }

    @Test
    void invalidJsonReturnsClearError() {
        assertThatThrownBy(() -> parser.parse("{not json"))
                .isInstanceOf(AgentPlanValidationException.class)
                .hasMessageContaining("not valid JSON");
    }

    @Test
    void unknownIntentReturnsClearError() {
        String json = validPlanJson().replace("RETURN_AND_REFUND", "MAGIC_REFUND");

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AgentPlanValidationException.class)
                .hasMessageContaining("Unsupported intent");
    }

    @Test
    void emptyPolicyQueryReturnsClearError() {
        String json = validPlanJson().replace("\"质量问题 退货 退款\"", "\"\"");

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AgentPlanValidationException.class)
                .hasMessageContaining("policyQuery");
    }

    @Test
    void unknownToolNameIsRejectedByPlanValidator() {
        String json = validPlanJson().replace("search_aftersale_policy", "issue_refund");
        AgentPlan plan = parser.parse(json);

        assertThatThrownBy(() -> AgentPlanValidator.validate(plan, List.of("search_aftersale_policy",
                "add_ticket_note")))
                .isInstanceOf(AgentPlanValidationException.class)
                .hasMessageContaining("not allowed for current AgentRun");
    }

    @Test
    void unsafeRefundCompletionClaimIsRejected() {
        String json = validPlanJson().replace("建议用户提供故障凭证后进入退货退款审核流程", "已退款，请用户查收");
        AgentPlan plan = parser.parse(json);

        assertThatThrownBy(() -> AgentPlanValidator.validate(plan, List.of("search_aftersale_policy",
                "add_ticket_note")))
                .isInstanceOf(AgentPlanValidationException.class)
                .hasMessageContaining("unsafe high-risk completion claim");
    }

    @Test
    void parsesValidAgentPlanJsonWithSubtasks() {
        AgentPlan plan = parser.parse(validMultiIntentPlanJson());

        assertThat(plan.intent()).isEqualTo(IntentType.MULTI_INTENT);
        assertThat(plan.subtasks()).hasSize(3);
        assertThat(plan.subtasks())
                .extracting("type")
                .containsExactly(SubtaskType.RETURN, SubtaskType.EXCHANGE, SubtaskType.COUPON_CONSULTATION);
        assertThat(plan.subtasks().get(0).plannedTools())
                .extracting("toolName")
                .containsExactly("get_order_by_id", "search_aftersale_policy", "add_ticket_note");
    }

    @Test
    void unknownSubtaskTypeIsRejectedByPlanValidator() {
        String json = validMultiIntentPlanJson().replace("\"RETURN\"", "\"MAGIC_RETURN\"");
        AgentPlan plan = parser.parse(json);

        assertThatThrownBy(() -> AgentPlanValidator.validate(plan, allToolNames()))
                .isInstanceOf(AgentPlanValidationException.class)
                .hasMessageContaining("unknown subtask type");
    }

    @Test
    void unknownSubtaskPlannedToolIsRejectedByPlanValidator() {
        String json = validMultiIntentPlanJson().replace("get_order_by_id", "issue_refund");
        AgentPlan plan = parser.parse(json);

        assertThatThrownBy(() -> AgentPlanValidator.validate(plan, allToolNames()))
                .isInstanceOf(AgentPlanValidationException.class)
                .hasMessageContaining("not allowed for current AgentRun");
    }

    @Test
    void emptySubtaskPolicyQueryIsRejectedByPlanValidator() {
        AgentPlan plan = invalidSubtaskPlan(new AgentSubtask(
                "subtask-empty-policy",
                SubtaskType.RETURN,
                "有污渍的衣服",
                "其中一件有污渍要退货",
                1,
                ToolRiskLevel.MEDIUM,
                "",
                defaultSubtaskTools(),
                List.of()));

        assertThatThrownBy(() -> AgentPlanValidator.validate(plan, allToolNames()))
                .isInstanceOf(AgentPlanValidationException.class)
                .hasMessageContaining("policyQuery");
    }

    @Test
    void missingSubtaskDependencyIsRejectedByPlanValidator() {
        AgentPlan plan = invalidSubtaskPlan(new AgentSubtask(
                "subtask-missing-dependency",
                SubtaskType.EXCHANGE,
                "需要换尺码的衣服",
                "另一件要换尺码",
                2,
                ToolRiskLevel.MEDIUM,
                "换货 尺码不合适",
                defaultSubtaskTools(),
                List.of("subtask-not-found")));

        assertThatThrownBy(() -> AgentPlanValidator.validate(plan, allToolNames()))
                .isInstanceOf(AgentPlanValidationException.class)
                .hasMessageContaining("unknown subtaskId");
    }

    @Test
    void cyclicSubtaskDependenciesAreRejectedByPlanValidator() {
        AgentPlan plan = invalidSubtaskPlan(
                new AgentSubtask(
                        "subtask-a",
                        SubtaskType.RETURN,
                        "有污渍的衣服",
                        "其中一件有污渍要退货",
                        1,
                        ToolRiskLevel.MEDIUM,
                        "质量问题 退货",
                        defaultSubtaskTools(),
                        List.of("subtask-b")),
                new AgentSubtask(
                        "subtask-b",
                        SubtaskType.EXCHANGE,
                        "需要换尺码的衣服",
                        "另一件要换尺码",
                        2,
                        ToolRiskLevel.MEDIUM,
                        "换货 尺码不合适",
                        defaultSubtaskTools(),
                        List.of("subtask-a")));

        assertThatThrownBy(() -> AgentPlanValidator.validate(plan, allToolNames()))
                .isInstanceOf(AgentPlanValidationException.class)
                .hasMessageContaining("cycle");
    }

    static String validPlanJson() {
        return """
                {
                  "intent": "RETURN_AND_REFUND",
                  "riskLevel": "MEDIUM",
                  "policyQuery": "质量问题 退货 退款",
                  "noteToAdd": "用户反馈耳机左耳无声，建议根据质量问题退换货规则进入人工审核。",
                  "finalSuggestion": "该问题疑似质量问题，建议用户提供故障凭证后进入退货退款审核流程。",
                  "evidenceHints": [
                    "用户描述：耳机左耳无声",
                    "需检索质量问题退换货规则"
                  ],
                  "plannedTools": [
                    {
                      "toolName": "search_aftersale_policy",
                      "reason": "检索质量问题退换货规则"
                    },
                    {
                      "toolName": "add_ticket_note",
                      "reason": "写入 Agent 处理建议"
                    }
                  ]
                }
                """;
    }

    static String validMultiIntentPlanJson() {
        return """
                {
                  "intent": "MULTI_INTENT",
                  "riskLevel": "MEDIUM",
                  "policyQuery": "服装退货 换货 优惠券",
                  "noteToAdd": "用户一次提出退货、换货和优惠券咨询诉求，需按结构化子任务顺序处理。",
                  "finalSuggestion": "该售后问题包含退货、换货和优惠券咨询三个子任务。",
                  "evidenceHints": [
                    "用户一次性提出多个售后诉求",
                    "需要分别检索退货、换货、优惠券规则"
                  ],
                  "plannedTools": [
                    {
                      "toolName": "get_order_by_id",
                      "reason": "查询订单事实"
                    },
                    {
                      "toolName": "search_aftersale_policy",
                      "reason": "检索售后政策"
                    },
                    {
                      "toolName": "add_ticket_note",
                      "reason": "写入处理建议"
                    }
                  ],
                  "subtasks": [
                    {
                      "subtaskId": "subtask-1",
                      "type": "RETURN",
                      "target": "有污渍的衣服",
                      "userMessageFragment": "其中一件有污渍要退货",
                      "priority": 1,
                      "riskLevel": "MEDIUM",
                      "policyQuery": "质量问题 退货 污渍",
                      "plannedTools": [
                        {
                          "toolName": "get_order_by_id",
                          "reason": "查询订单事实"
                        },
                        {
                          "toolName": "search_aftersale_policy",
                          "reason": "检索质量问题退货政策"
                        },
                        {
                          "toolName": "add_ticket_note",
                          "reason": "记录退货子任务处理建议"
                        }
                      ],
                      "dependencies": []
                    },
                    {
                      "subtaskId": "subtask-2",
                      "type": "EXCHANGE",
                      "target": "需要换尺码的衣服",
                      "userMessageFragment": "另一件要换尺码",
                      "priority": 2,
                      "riskLevel": "MEDIUM",
                      "policyQuery": "换货 尺码不合适",
                      "plannedTools": [
                        {
                          "toolName": "get_order_by_id",
                          "reason": "查询订单事实"
                        },
                        {
                          "toolName": "search_aftersale_policy",
                          "reason": "检索尺码换货政策"
                        },
                        {
                          "toolName": "add_ticket_note",
                          "reason": "记录换货子任务处理建议"
                        }
                      ],
                      "dependencies": []
                    },
                    {
                      "subtaskId": "subtask-3",
                      "type": "COUPON_CONSULTATION",
                      "target": "未使用优惠券",
                      "userMessageFragment": "还有一张优惠券没用上怎么退",
                      "priority": 3,
                      "riskLevel": "LOW",
                      "policyQuery": "优惠券 未使用 退还",
                      "plannedTools": [
                        {
                          "toolName": "get_order_by_id",
                          "reason": "查询订单事实"
                        },
                        {
                          "toolName": "search_aftersale_policy",
                          "reason": "检索优惠券未使用规则"
                        },
                        {
                          "toolName": "add_ticket_note",
                          "reason": "记录优惠券咨询子任务处理建议"
                        }
                      ],
                      "dependencies": []
                    }
                  ]
                }
                """;
    }

    private static AgentPlan invalidSubtaskPlan(AgentSubtask... subtasks) {
        return new AgentPlan(
                IntentType.MULTI_INTENT,
                ToolRiskLevel.MEDIUM,
                "多意图售后",
                "多意图售后备注。",
                "多意图售后建议。",
                List.of("多意图售后测试。"),
                defaultSubtaskTools(),
                List.of(subtasks));
    }

    private static List<PlannedToolCall> defaultSubtaskTools() {
        return List.of(
                new PlannedToolCall("get_order_by_id", "查询订单事实"),
                new PlannedToolCall("search_aftersale_policy", "检索政策"),
                new PlannedToolCall("add_ticket_note", "写入备注"));
    }

    private static List<String> allToolNames() {
        return List.of("get_order_by_id", "search_aftersale_policy", "add_ticket_note");
    }
}
