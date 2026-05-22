package com.example.aftersale.policy.rag.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.aftersale.common.ai.SpringAiProviderProperties;
import com.example.aftersale.policy.rag.infrastructure.springai.SpringAiEmbeddingClient;
import com.example.aftersale.policy.rag.infrastructure.springai.SpringAiEmbeddingGateway;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmbeddingClientTest {

    @Test
    void fakeEmbeddingClientReturnsDeterministicVector() {
        FakeEmbeddingClient client = new FakeEmbeddingClient(4);
        EmbeddingRequest request = new EmbeddingRequest("fake-embedding", "quality return policy");

        EmbeddingResponse first = client.embed(request);
        EmbeddingResponse second = client.embed(request);

        assertThat(first.vector()).isEqualTo(second.vector());
        assertThat(first.dimension()).isEqualTo(4);
        assertThat(first.tokenEstimate()).isPositive();
    }

    @Test
    void springAiEmbeddingClientMapsTextToGateway() {
        RecordingEmbeddingGateway gateway = new RecordingEmbeddingGateway();
        SpringAiEmbeddingClient client = new SpringAiEmbeddingClient(enabledProperties(), gateway);

        EmbeddingResponse response = client.embed(new EmbeddingRequest(
                "spring-embedding",
                "policy text"));

        assertThat(gateway.text).isEqualTo("policy text");
        assertThat(response.model()).isEqualTo("spring-embedding");
        assertThat(response.dimension()).isEqualTo(3);
        assertThat(response.vector()).containsExactly(0.1d, 0.2d, 0.3d);
    }

    @Test
    void springAiEmbeddingDisabledFailsClearly() {
        SpringAiProviderProperties properties = enabledProperties();
        properties.setEmbeddingEnabled(false);
        SpringAiEmbeddingClient client = new SpringAiEmbeddingClient(properties, text -> List.of(0.1d));

        assertThatThrownBy(() -> client.embed(new EmbeddingRequest("spring-embedding", "policy text")))
                .isInstanceOf(EmbeddingProviderException.class)
                .hasMessageContaining("agent.spring-ai.embedding-enabled")
                .hasMessageContaining("provider=spring-ai");
    }

    private static SpringAiProviderProperties enabledProperties() {
        SpringAiProviderProperties properties = new SpringAiProviderProperties();
        properties.setEnabled(true);
        properties.setEmbeddingEnabled(true);
        properties.setProviderType("openai");
        properties.setEndpointHost("spring-ai-managed");
        return properties;
    }

    private static final class RecordingEmbeddingGateway implements SpringAiEmbeddingGateway {

        private String text;

        @Override
        public List<Double> embed(String text) {
            this.text = text;
            return List.of(0.1d, 0.2d, 0.3d);
        }
    }
}
