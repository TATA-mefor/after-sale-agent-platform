package io.github.tatame.aftersale.agent.infrastructure.llm;

import io.github.tatame.aftersale.agent.application.planner.AgentPlanValidationException;

/**
 * 保存一次 Provider 调用所需的最小配置。
 *
 * <p>边界：该对象可以携带 API Key 供传输层使用，但不得被写入日志、trace、workspace 或持久化结果。
 */
public record LlmProviderSettings(
        LlmProvider provider,
        String model,
        String apiKey,
        String endpoint,
        int timeoutSeconds) {

    public LlmProviderSettings {
        if (provider == null) {
            throw new AgentPlanValidationException("LLM provider must not be null");
        }
        if (model == null || model.isBlank()) {
            throw new AgentPlanValidationException("LLM model must not be blank");
        }
        if (apiKey == null) {
            throw new AgentPlanValidationException("LLM apiKey must not be null");
        }
        if (endpoint == null || endpoint.isBlank()) {
            throw new AgentPlanValidationException("LLM endpoint must not be blank");
        }
        if (timeoutSeconds <= 0) {
            throw new AgentPlanValidationException("LLM timeoutSeconds must be positive");
        }
    }
}
