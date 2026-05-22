package com.example.aftersale.agent.infrastructure.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Thin Spring AI ChatClient wrapper. It does not register project tools as Spring AI tool callbacks.
 */
final class ChatClientSpringAiChatGateway implements SpringAiChatGateway {

    private final ObjectProvider<ChatClient.Builder> builderProvider;

    ChatClientSpringAiChatGateway(ObjectProvider<ChatClient.Builder> builderProvider) {
        this.builderProvider = builderProvider;
    }

    @Override
    public String complete(String model, String systemPrompt, String userPrompt) {
        ChatClient.Builder builder = builderProvider.getIfAvailable();
        if (builder == null) {
            throw new IllegalStateException("Spring AI ChatClient.Builder bean is not available");
        }
        return builder.clone()
                .build()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }
}
