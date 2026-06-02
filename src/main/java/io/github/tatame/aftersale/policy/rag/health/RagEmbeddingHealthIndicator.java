package io.github.tatame.aftersale.policy.rag.health;

import io.github.tatame.aftersale.common.ai.SpringAiProviderProperties;
import io.github.tatame.aftersale.policy.rag.application.EmbeddingClient;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "agent.rag.health", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagEmbeddingHealthIndicator implements HealthIndicator {

    private final ObjectProvider<List<EmbeddingClient>> embeddingClientsProvider;
    private final ObjectProvider<SpringAiProviderProperties> springAiPropertiesProvider;
    private final boolean includeDetails;

    public RagEmbeddingHealthIndicator(
            ObjectProvider<List<EmbeddingClient>> embeddingClientsProvider,
            ObjectProvider<SpringAiProviderProperties> springAiPropertiesProvider,
            RagHealthProperties properties) {
        this.embeddingClientsProvider = embeddingClientsProvider;
        this.springAiPropertiesProvider = springAiPropertiesProvider;
        this.includeDetails = properties.isIncludeDetails();
    }

    @Override
    public Health health() {
        SpringAiProviderProperties springAiProperties = springAiPropertiesProvider.getIfAvailable();
        List<EmbeddingClient> clients = embeddingClientsProvider.getIfAvailable(List::of);
        boolean springAiEmbeddingEnabled = springAiProperties != null
                && springAiProperties.isEnabled()
                && springAiProperties.isEmbeddingEnabled();
        if (springAiEmbeddingEnabled) {
            return springAiHealth(springAiProperties, clients);
        }
        return disabledHealth(springAiProperties, clients);
    }

    private Health springAiHealth(SpringAiProviderProperties springAiProperties, List<EmbeddingClient> clients) {
        boolean apiKeyConfigured = !RagHealthDetailSanitizer.isBlank(springAiProperties.getApiKey());
        boolean clientAvailable = !clients.isEmpty();
        Health.Builder builder = apiKeyConfigured && clientAvailable ? Health.status(Status.UNKNOWN) : Health.down();
        if (includeDetails) {
            baseDetails(builder, clients)
                    .withDetail("provider", "spring-ai")
                    .withDetail("providerType", RagHealthDetailSanitizer.provider(springAiProperties.getProviderType()))
                    .withDetail("endpointHost", RagHealthDetailSanitizer.safeEndpointHost(
                            springAiProperties.getEndpointHost()))
                    .withDetail("apiKeyConfigured", apiKeyConfigured)
                    .withDetail("message", apiKeyConfigured && clientAvailable
                            ? "Spring AI embedding is configured; live provider calls are not checked."
                            : "Spring AI embedding is enabled but required configuration or client bean is missing.");
        }
        return builder.build();
    }

    private Health disabledHealth(SpringAiProviderProperties springAiProperties, List<EmbeddingClient> clients) {
        Health.Builder builder = Health.up();
        if (includeDetails) {
            baseDetails(builder, clients)
                    .withDetail("provider", "disabled")
                    .withDetail("disabled", true)
                    .withDetail("springAiEnabled", springAiProperties != null && springAiProperties.isEnabled())
                    .withDetail("springAiEmbeddingEnabled", springAiProperties != null
                            && springAiProperties.isEmbeddingEnabled())
                    .withDetail("message", "Embedding provider is disabled or fake-only; no embedding call was made.");
        }
        return builder.build();
    }

    private Health.Builder baseDetails(Health.Builder builder, List<EmbeddingClient> clients) {
        return builder.withDetail("component", "rag-embedding")
                .withDetail("embeddingClientBeans", clients.size())
                .withDetail("offlineReadinessOnly", true)
                .withDetail("embeddingCallExecuted", false)
                .withDetail("springAiCallExecuted", false);
    }
}
