package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.example.aftersale.agent.application.planner.AgentPlan;
import com.example.aftersale.agent.application.planner.AgentPlanningContext;
import com.example.aftersale.agent.infrastructure.llm.AgentPlanParser;
import com.example.aftersale.agent.infrastructure.llm.AgentPlannerProperties;
import com.example.aftersale.agent.infrastructure.llm.LlmAgentPlanner;
import com.example.aftersale.agent.infrastructure.llm.LlmClientFactory;
import com.example.aftersale.agent.infrastructure.llm.LlmProvider;
import com.example.aftersale.agent.prompt.AgentPlannerPromptFactory;
import com.example.aftersale.ticket.domain.TicketStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * 验证 live LLM Planner 的 opt-in 冒烟路径，避免默认离线测试依赖真实 Provider 或 API Key。
 */
@Tag("live")
@EnabledIfSystemProperty(named = "live.llm", matches = "true")
class LlmPlannerLiveSmokeTest {

    private static final List<String> AVAILABLE_TOOLS = List.of("search_aftersale_policy", "add_ticket_note");

    @Test
    void llmPlannerCanCallRealProviderAndReturnValidatedAgentPlan() {
        ObjectMapper objectMapper = new ObjectMapper();
        AgentPlannerProperties.Llm properties = liveProperties();
        LlmProvider provider = LlmProvider.from(properties.getProvider());
        assumeTrue(liveProviderCredentialsPresent(provider, properties),
                "Set provider API key and run with -Dlive.llm=true to execute the live LLM smoke test.");
        properties.setTimeoutSeconds(60);
        LlmClientFactory clientFactory = new LlmClientFactory(objectMapper);

        LlmAgentPlanner planner = new LlmAgentPlanner(
                properties,
                clientFactory.create(properties),
                new AgentPlanParser(objectMapper),
                new AgentPlannerPromptFactory(objectMapper));

        AgentPlan plan = planner.plan(planningContext());

        assertThat(plan.intent()).isNotNull();
        assertThat(plan.riskLevel()).isNotNull();
        assertThat(plan.policyQuery()).isNotBlank();
        assertThat(plan.noteToAdd()).isNotBlank();
        assertThat(plan.finalSuggestion()).isNotBlank();
        assertThat(plan.plannedTools())
                .isNotEmpty()
                .allSatisfy(tool -> assertThat(AVAILABLE_TOOLS).contains(tool.toolName()));
    }

    private static AgentPlanningContext planningContext() {
        return new AgentPlanningContext(
                "T-LIVE-SMOKE-1",
                "U-LIVE-SMOKE-1",
                "O-LIVE-SMOKE-1",
                "我买的耳机用了两天左耳没有声音，想退货退款，请帮我判断下一步。",
                TicketStatus.CREATED,
                AVAILABLE_TOOLS,
                "LLM may only plan. High-risk actions such as refund, compensation, and dispute closure require "
                        + "human approval and must not be claimed as completed.",
                Instant.parse("2026-05-14T00:00:00Z"));
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static AgentPlannerProperties.Llm liveProperties() {
        AgentPlannerProperties.Llm properties = new AgentPlannerProperties.Llm();
        properties.setProvider(envOrDefault("AFTERSALE_LLM_PROVIDER", "openai-responses"));
        properties.setModel(envOrDefault("AFTERSALE_LLM_MODEL", "gpt-4.1-mini"));
        properties.setApiKey(envOrDefault("OPENAI_API_KEY", ""));
        properties.setEndpoint(envOrDefault("OPENAI_RESPONSES_ENDPOINT",
                "https://api.openai.com/v1/responses"));
        properties.getDashscope().setApiKey(envOrDefault("DASHSCOPE_API_KEY", ""));
        properties.getDashscope().setBaseUrl(envOrDefault("DASHSCOPE_BASE_URL",
                "https://dashscope.aliyuncs.com/api/v2/apps/protocols/compatible-mode/v1"));
        properties.getDashscope().setResponsesEndpoint(envOrDefault("DASHSCOPE_RESPONSES_ENDPOINT", ""));
        properties.getDashscope().setChatCompletionsEndpoint(
                envOrDefault("DASHSCOPE_CHAT_COMPLETIONS_ENDPOINT", ""));
        return properties;
    }

    private static boolean liveProviderCredentialsPresent(
            LlmProvider provider,
            AgentPlannerProperties.Llm properties) {
        return switch (provider) {
            case OPENAI_RESPONSES -> !properties.getApiKey().isBlank();
            case DASHSCOPE_RESPONSES, DASHSCOPE_CHAT_COMPATIBLE ->
                    !properties.getDashscope().getApiKey().isBlank();
        };
    }
}
