package com.example.aftersale.agent.infrastructure.llm;

import com.example.aftersale.agent.application.planner.AgentPlanner;
import com.example.aftersale.agent.application.planner.FakeAgentPlanner;
import com.example.aftersale.agent.application.planner.RuleBasedAgentPlanner;
import com.example.aftersale.agent.prompt.AgentPlannerPromptFactory;
import com.example.aftersale.agent.prompt.CompactToolCatalogBuilder;
import com.example.aftersale.agent.prompt.PromptBudget;
import com.example.aftersale.agent.prompt.PromptBudgetApplier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 组装当前运行模式下使用的 AgentPlanner。
 *
 * <p>边界：配置层只选择规则、假实现或 LLM Planner；默认模式仍必须保持离线可运行，LLM 模式必须显式配置
 * Provider 凭证后才会启用。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AgentPlannerProperties.class)
public class AgentPlannerConfiguration {

    /**
     * 创建唯一的规划器入口。
     *
     * <p>LLM 模式在构造前检查 Provider 配置，避免运行到业务编排中途才因为缺少 API Key 失败。
     */
    @Bean
    public AgentPlanner agentPlanner(AgentPlannerProperties properties, ObjectMapper objectMapper) {
        return switch (properties.getMode()) {
            case RULE -> new RuleBasedAgentPlanner();
            case FAKE -> new FakeAgentPlanner();
            case LLM -> {
                LlmClientFactory llmClientFactory = new LlmClientFactory(objectMapper);
                LlmProvider provider = LlmProvider.from(properties.getLlm().getProvider());
                validateLlmConfiguration(llmClientFactory.settings(provider, properties.getLlm()));
                yield new LlmAgentPlanner(
                        properties.getLlm(),
                        llmClientFactory.create(properties.getLlm()),
                        new AgentPlanParser(objectMapper),
                        new AgentPlannerPromptFactory(
                                objectMapper,
                                new PromptBudgetApplier(),
                                promptBudget(properties.getLlm().getBudget()),
                                new CompactToolCatalogBuilder(objectMapper)));
            }
        };
    }

    private static PromptBudget promptBudget(AgentPlannerProperties.Budget budget) {
        return new PromptBudget(
                budget.getSystemPromptTokens(),
                budget.getHistoryTokens(),
                budget.getRagContextTokens(),
                budget.getToolCatalogTokens(),
                budget.getMaxOutputTokens(),
                budget.getTotalInputTokens());
    }

    private static void validateLlmConfiguration(LlmProviderSettings settings) {
        if (settings.apiKey() == null || settings.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "agent.planner.mode=llm requires a provider API key. Use OPENAI_API_KEY for openai-responses "
                            + "or DASHSCOPE_API_KEY for dashscope providers.");
        }
    }
}
