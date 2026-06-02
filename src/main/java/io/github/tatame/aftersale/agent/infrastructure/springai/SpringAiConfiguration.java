package io.github.tatame.aftersale.agent.infrastructure.springai;

import io.github.tatame.aftersale.common.ai.SpringAiProviderProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SpringAiProviderProperties.class)
public class SpringAiConfiguration {

    @Bean
    public SpringAiLlmClient springAiLlmClient(
            SpringAiProviderProperties properties,
            ObjectProvider<SpringAiChatGateway> chatGatewayProvider) {
        return new SpringAiLlmClient(properties, chatGatewayProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "agent.spring-ai",
            name = {"enabled", "chat-enabled"},
            havingValue = "true")
    public SpringAiChatGateway springAiChatGateway(ObjectProvider<ChatClient.Builder> builderProvider) {
        return new ChatClientSpringAiChatGateway(builderProvider);
    }
}
