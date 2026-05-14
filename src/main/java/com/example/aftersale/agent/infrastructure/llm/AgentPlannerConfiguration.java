package com.example.aftersale.agent.infrastructure.llm;

import com.example.aftersale.agent.application.planner.AgentPlanner;
import com.example.aftersale.agent.application.planner.FakeAgentPlanner;
import com.example.aftersale.agent.application.planner.RuleBasedAgentPlanner;
import com.example.aftersale.agent.prompt.AgentPlannerPromptFactory;
import com.example.aftersale.agent.infrastructure.llm.AgentPlannerProperties.Llm;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AgentPlannerProperties.class)
public class AgentPlannerConfiguration {

    @Bean
    public AgentPlanner agentPlanner(AgentPlannerProperties properties, ObjectMapper objectMapper) {
        return switch (properties.getMode()) {
            case RULE -> new RuleBasedAgentPlanner();
            case FAKE -> new FakeAgentPlanner();
            case LLM -> {
                validateLlmConfiguration(properties.getLlm());
                yield new LlmAgentPlanner(
                        properties.getLlm(),
                        new OpenAiLlmClient(properties.getLlm(), objectMapper),
                        new AgentPlanParser(objectMapper),
                        new AgentPlannerPromptFactory(objectMapper));
            }
        };
    }

    private static void validateLlmConfiguration(Llm llm) {
        if (llm.getApiKey() == null || llm.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "agent.planner.mode=llm requires agent.planner.llm.api-key or OPENAI_API_KEY");
        }
    }
}
