package com.example.aftersale.policy.rag.infrastructure.springai;

import com.example.aftersale.common.ai.SpringAiProviderProperties;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SpringAiProviderProperties.class)
public class SpringAiEmbeddingConfiguration {

    @Bean
    public SpringAiEmbeddingClient springAiEmbeddingClient(
            SpringAiProviderProperties properties,
            ObjectProvider<SpringAiEmbeddingGateway> embeddingGatewayProvider) {
        return new SpringAiEmbeddingClient(properties, embeddingGatewayProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "agent.spring-ai",
            name = {"enabled", "embedding-enabled"},
            havingValue = "true")
    public SpringAiEmbeddingGateway springAiEmbeddingGateway(ObjectProvider<EmbeddingModel> embeddingModelProvider) {
        return new EmbeddingModelSpringAiEmbeddingGateway(embeddingModelProvider);
    }
}
