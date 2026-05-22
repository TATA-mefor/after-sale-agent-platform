package com.example.aftersale.agent.infrastructure.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.example.aftersale.agent.application.planner.AgentPlan;
import com.example.aftersale.agent.application.planner.AgentPlanningContext;
import com.example.aftersale.agent.application.planner.AgentPlanValidationException;
import com.example.aftersale.agent.infrastructure.llm.AgentPlanParser;
import com.example.aftersale.agent.infrastructure.llm.AgentPlannerProperties;
import com.example.aftersale.agent.infrastructure.llm.LlmAgentPlanner;
import com.example.aftersale.agent.infrastructure.llm.LlmRequest;
import com.example.aftersale.agent.infrastructure.llm.LlmResponse;
import com.example.aftersale.agent.prompt.AgentPlannerPromptFactory;
import com.example.aftersale.common.ai.SpringAiProviderProperties;
import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.ticket.domain.TicketStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpringAiLlmClientTest {

    private final RecordingGateway gateway = new RecordingGateway();

    @Test
    void requestMapsSystemAndUserPromptToSpringAiGateway() {
        SpringAiLlmClient client = new SpringAiLlmClient(enabledProperties(), gateway);

        LlmResponse response = client.complete(new LlmRequest(
                "spring-test-model",
                "system prompt",
                "user prompt",
                30));

        assertThat(response.rawContent()).contains("RETURN_AND_REFUND");
        assertThat(gateway.model).isEqualTo("spring-test-model");
        assertThat(gateway.systemPrompt).isEqualTo("system prompt");
        assertThat(gateway.userPrompt).isEqualTo("user prompt");
    }

    @Test
    void springAiDisabledFailsBeforeProviderCall() {
        SpringAiProviderProperties properties = enabledProperties();
        properties.setEnabled(false);
        SpringAiLlmClient client = new SpringAiLlmClient(properties, gateway);

        assertThatThrownBy(() -> client.complete(new LlmRequest(
                "spring-test-model",
                "system prompt",
                "user prompt",
                30)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("agent.spring-ai.enabled")
                .hasMessageContaining("provider=spring-ai");
    }

    @Test
    void providerErrorSummaryIsSanitized() {
        SpringAiProviderProperties properties = enabledProperties();
        properties.setApiKey("test-secret-api-key");
        RecordingGateway failingGateway = new RecordingGateway();
        failingGateway.exception = new IllegalStateException(
                "failure with test-secret-api-key and full prompt: user prompt");
        SpringAiLlmClient client = new SpringAiLlmClient(properties, failingGateway);

        Throwable thrown = catchThrowable(() -> client.complete(new LlmRequest(
                "spring-test-model",
                "system prompt",
                "user prompt",
                30)));

        assertThat(thrown)
                .isInstanceOf(AgentPlanValidationException.class);
        assertThat(thrown.getMessage())
                .contains("provider=spring-ai-chat")
                .contains("model=spring-test-model")
                .contains("errorClass=IllegalStateException")
                .doesNotContain("test-secret-api-key")
                .doesNotContain("system prompt");
    }

    @Test
    void llmAgentPlannerStillParsesAndValidatesSpringAiTextOnly() {
        ObjectMapper objectMapper = new ObjectMapper();
        AgentPlannerProperties.Llm plannerProperties = new AgentPlannerProperties.Llm();
        plannerProperties.setModel("spring-test-model");
        SpringAiLlmClient client = new SpringAiLlmClient(enabledProperties(), gateway);
        LlmAgentPlanner planner = new LlmAgentPlanner(
                plannerProperties,
                client,
                new AgentPlanParser(objectMapper),
                new AgentPlannerPromptFactory(objectMapper));

        AgentPlan plan = planner.plan(planningContext());

        assertThat(plan.intent()).isEqualTo(IntentType.RETURN_AND_REFUND);
        assertThat(plan.plannedTools()).extracting("toolName")
                .containsExactly("search_aftersale_policy", "add_ticket_note");
        assertThat(gateway.userPrompt).contains("T-SPRING-AI-1");
    }

    private static SpringAiProviderProperties enabledProperties() {
        SpringAiProviderProperties properties = new SpringAiProviderProperties();
        properties.setEnabled(true);
        properties.setChatEnabled(true);
        properties.setProviderType("openai");
        properties.setEndpointHost("spring-ai-managed");
        return properties;
    }

    private static AgentPlanningContext planningContext() {
        return new AgentPlanningContext(
                "T-SPRING-AI-1",
                "U-SPRING-AI-1",
                "O-SPRING-AI-1",
                "耳机左耳无声，想退货退款。",
                TicketStatus.CREATED,
                List.of("search_aftersale_policy", "add_ticket_note"),
                "High-risk actions require human approval.",
                Instant.parse("2026-05-14T00:00:00Z"));
    }

    private static final class RecordingGateway implements SpringAiChatGateway {

        private String model;

        private String systemPrompt;

        private String userPrompt;

        private RuntimeException exception;

        @Override
        public String complete(String model, String systemPrompt, String userPrompt) {
            this.model = model;
            this.systemPrompt = systemPrompt;
            this.userPrompt = userPrompt;
            if (exception != null) {
                throw exception;
            }
            return validPlanJson();
        }
    }

    private static String validPlanJson() {
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
