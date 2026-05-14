package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.aftersale.agent.application.planner.AgentPlan;
import com.example.aftersale.agent.application.planner.AgentPlanValidationException;
import com.example.aftersale.agent.application.planner.AgentPlanValidator;
import com.example.aftersale.agent.infrastructure.llm.AgentPlanParser;
import com.example.aftersale.ticket.domain.IntentType;
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
                .hasMessageContaining("unknown tool");
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
}
