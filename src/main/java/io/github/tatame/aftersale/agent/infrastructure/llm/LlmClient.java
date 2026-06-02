package io.github.tatame.aftersale.agent.infrastructure.llm;

public interface LlmClient {

    LlmResponse complete(LlmRequest request);
}
