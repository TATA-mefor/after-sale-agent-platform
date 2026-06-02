package io.github.tatame.aftersale.agent.infrastructure.llm;

import io.github.tatame.aftersale.agent.application.planner.AgentPlanValidationException;
import java.util.Objects;

public record LlmResponse(String rawContent) {

    public LlmResponse {
        rawContent = Objects.requireNonNull(rawContent, "rawContent must not be null");
        if (rawContent.isBlank()) {
            throw new AgentPlanValidationException("rawContent must not be blank");
        }
    }
}
