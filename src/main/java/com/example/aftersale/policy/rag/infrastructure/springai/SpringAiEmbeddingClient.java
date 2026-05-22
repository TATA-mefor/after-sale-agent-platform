package com.example.aftersale.policy.rag.infrastructure.springai;

import com.example.aftersale.common.ai.SpringAiProviderErrorFormatter;
import com.example.aftersale.common.ai.SpringAiProviderProperties;
import com.example.aftersale.policy.rag.application.EmbeddingClient;
import com.example.aftersale.policy.rag.application.EmbeddingProviderException;
import com.example.aftersale.policy.rag.application.EmbeddingRequest;
import com.example.aftersale.policy.rag.application.EmbeddingResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;

public class SpringAiEmbeddingClient implements EmbeddingClient {

    private static final String PROVIDER_NAME = "spring-ai-embedding";

    private final SpringAiProviderProperties properties;

    private final Optional<SpringAiEmbeddingGateway> embeddingGateway;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Configuration properties are managed by Spring and read during provider calls.")
    public SpringAiEmbeddingClient(
            SpringAiProviderProperties properties,
            SpringAiEmbeddingGateway embeddingGateway) {
        this.properties = properties;
        this.embeddingGateway = Optional.ofNullable(embeddingGateway);
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        validateConfiguration();
        try {
            List<Double> vector = embeddingGateway.orElseThrow().embed(request.text());
            return new EmbeddingResponse(
                    request.model(),
                    vector.size(),
                    vector,
                    estimateTokens(request.text()));
        } catch (RuntimeException exception) {
            throw new EmbeddingProviderException(SpringAiProviderErrorFormatter.format(
                    PROVIDER_NAME,
                    request.model(),
                    properties,
                    exception), exception);
        }
    }

    public void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new EmbeddingProviderException(SpringAiProviderErrorFormatter.configuration(
                    "agent.spring-ai.enabled must be true when using Spring AI embedding",
                    properties));
        }
        if (!properties.isEmbeddingEnabled()) {
            throw new EmbeddingProviderException(SpringAiProviderErrorFormatter.configuration(
                    "agent.spring-ai.embedding-enabled must be true when using Spring AI embedding",
                    properties));
        }
        if (embeddingGateway.isEmpty()) {
            throw new EmbeddingProviderException(SpringAiProviderErrorFormatter.configuration(
                    "Spring AI embedding gateway is not available; check EmbeddingModel configuration",
                    properties));
        }
    }

    private static int estimateTokens(String text) {
        return Math.max(1, text.length() / 4);
    }
}
