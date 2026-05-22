package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.aftersale.agent.application.planner.AgentPlan;
import com.example.aftersale.agent.application.planner.AgentPlanningContext;
import com.example.aftersale.agent.infrastructure.llm.AgentPlanParser;
import com.example.aftersale.agent.infrastructure.llm.AgentPlannerProperties;
import com.example.aftersale.agent.infrastructure.llm.LlmAgentPlanner;
import com.example.aftersale.agent.infrastructure.llm.LlmClient;
import com.example.aftersale.agent.infrastructure.llm.LlmRequest;
import com.example.aftersale.agent.infrastructure.llm.LlmResponse;
import com.example.aftersale.agent.prompt.AgentPlannerPromptFactory;
import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.ticket.domain.TicketStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LlmAgentPlannerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void llmAgentPlannerUsesLlmClientAndParsesAgentPlan() {
        RecordingLlmClient llmClient = new RecordingLlmClient();
        AgentPlannerProperties.Llm properties = new AgentPlannerProperties.Llm();
        properties.setApiKey("test-key-not-real");
        properties.setModel("test-model");
        LlmAgentPlanner planner = new LlmAgentPlanner(
                properties,
                llmClient,
                new AgentPlanParser(objectMapper),
                new AgentPlannerPromptFactory(objectMapper));

        AgentPlan plan = planner.plan(planningContext());

        assertThat(plan.intent()).isEqualTo(IntentType.RETURN_AND_REFUND);
        assertThat(plan.policyQuery()).isEqualTo("质量问题 退货 退款");
        assertThat(llmClient.lastRequest.model()).isEqualTo("test-model");
        assertThat(llmClient.lastRequest.systemPrompt()).contains("Return only one JSON object");
        assertThat(llmClient.lastRequest.userPrompt()).contains("T-LLM-1");
        assertThat(llmClient.lastRequest.userPrompt())
                .contains("get_order_by_id", "search_aftersale_policy", "add_ticket_note")
                .doesNotContain("create_aftersale_ticket", "update_ticket_status", "get_user_orders");
    }

    private static AgentPlanningContext planningContext() {
        return new AgentPlanningContext(
                "T-LLM-1",
                "U-LLM-1",
                "O-LLM-1",
                "我买的耳机有质量问题，想退货退款。",
                TicketStatus.CREATED,
                List.of("get_order_by_id", "search_aftersale_policy", "add_ticket_note"),
                "High-risk actions require human approval.",
                Instant.parse("2026-05-14T00:00:00Z"));
    }

    private static final class RecordingLlmClient implements LlmClient {

        private LlmRequest lastRequest;

        @Override
        public LlmResponse complete(LlmRequest request) {
            this.lastRequest = request;
            return new LlmResponse(AgentPlanParserTest.validPlanJson());
        }
    }
}
