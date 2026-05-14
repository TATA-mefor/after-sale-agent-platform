package com.example.aftersale.agent.infrastructure.llm;

public interface LlmClient {

    LlmResponse complete(LlmRequest request);
}
