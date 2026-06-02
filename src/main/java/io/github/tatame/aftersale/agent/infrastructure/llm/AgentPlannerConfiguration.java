package io.github.tatame.aftersale.agent.infrastructure.llm;

import io.github.tatame.aftersale.agent.application.planner.AgentPlanner;
import io.github.tatame.aftersale.agent.application.planner.FakeAgentPlanner;
import io.github.tatame.aftersale.agent.application.planner.RuleBasedAgentPlanner;
import io.github.tatame.aftersale.agent.prompt.AgentPlannerPromptFactory;
import io.github.tatame.aftersale.agent.prompt.CompactToolCatalogBuilder;
import io.github.tatame.aftersale.agent.prompt.PromptBudget;
import io.github.tatame.aftersale.agent.prompt.PromptBudgetApplier;
import io.github.tatame.aftersale.agent.infrastructure.springai.SpringAiLlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
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
    public AgentPlanner agentPlanner(
            AgentPlannerProperties properties,
            ObjectMapper objectMapper,
            ObjectProvider<SpringAiLlmClient> springAiLlmClientProvider) {
        return switch (properties.getMode()) {
            case RULE -> new RuleBasedAgentPlanner();
            case FAKE -> new FakeAgentPlanner();
            case LLM -> {
                LlmClientFactory llmClientFactory = new LlmClientFactory(
                        objectMapper,
                        springAiLlmClientProvider.getIfAvailable());
                LlmProvider provider = LlmProvider.from(properties.getLlm().getProvider());
                validateLlmConfiguration(provider, llmClientFactory, properties.getLlm());
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

    private static void validateLlmConfiguration(
            LlmProvider provider,
            LlmClientFactory llmClientFactory,
            AgentPlannerProperties.Llm properties) {
        if (provider == LlmProvider.SPRING_AI_CHAT) {
            LlmClient client = llmClientFactory.create(properties);
            if (client instanceof SpringAiLlmClient springAiLlmClient) {
                springAiLlmClient.validateConfiguration();
                return;
            }
            throw new IllegalStateException("provider=spring-ai-chat requires SpringAiLlmClient configuration");
        }
        validateHttpProviderSettings(llmClientFactory.settings(provider, properties));
    }

    private static void validateHttpProviderSettings(LlmProviderSettings settings) {
        if (settings.apiKey() == null || settings.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "agent.planner.mode=llm requires a provider API key. Use OPENAI_API_KEY for openai-responses "
                            + "or DASHSCOPE_API_KEY for dashscope providers.");
        }
    }
}
