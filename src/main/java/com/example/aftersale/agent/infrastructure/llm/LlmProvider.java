package com.example.aftersale.agent.infrastructure.llm;

import com.example.aftersale.agent.application.planner.AgentPlanValidationException;
import java.util.Locale;

public enum LlmProvider {

    OPENAI_RESPONSES("openai-responses"),
    DASHSCOPE_RESPONSES("dashscope-responses"),
    DASHSCOPE_CHAT_COMPATIBLE("dashscope-chat-compatible");

    private final String configValue;

    LlmProvider(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public static LlmProvider from(String value) {
        if (value == null || value.isBlank()) {
            return OPENAI_RESPONSES;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("openai".equals(normalized)) {
            return OPENAI_RESPONSES;
        }
        for (LlmProvider provider : values()) {
            if (provider.configValue.equals(normalized)) {
                return provider;
            }
        }
        throw new AgentPlanValidationException("Unsupported LLM provider: " + value);
    }
}
