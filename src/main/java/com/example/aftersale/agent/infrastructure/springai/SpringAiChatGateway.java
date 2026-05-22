package com.example.aftersale.agent.infrastructure.springai;

public interface SpringAiChatGateway {

    String complete(String model, String systemPrompt, String userPrompt);
}
