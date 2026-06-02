package io.github.tatame.aftersale.agent.infrastructure.llm;

import io.github.tatame.aftersale.agent.application.planner.AgentPlanValidationException;
import java.util.Objects;

public record LlmRequest(
        String model,
        String systemPrompt,
        String userPrompt,
        int timeoutSeconds) {

    public LlmRequest {
        model = requireText(model, "model");
        systemPrompt = requireText(systemPrompt, "systemPrompt");
        userPrompt = requireText(userPrompt, "userPrompt");
        if (timeoutSeconds <= 0) {
            throw new AgentPlanValidationException("timeoutSeconds must be positive");
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new AgentPlanValidationException(fieldName + " must not be blank");
        }
        return value;
    }
}
