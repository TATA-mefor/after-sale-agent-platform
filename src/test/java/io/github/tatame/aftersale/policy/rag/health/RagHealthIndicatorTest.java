package io.github.tatame.aftersale.policy.rag.health;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.tatame.aftersale.common.ai.SpringAiProviderProperties;
import io.github.tatame.aftersale.common.observability.metrics.ApplicationMetricsRecorder;
import io.github.tatame.aftersale.policy.application.PolicyApplicationService;
import io.github.tatame.aftersale.policy.infrastructure.repository.InMemoryPolicyRepository;
import io.github.tatame.aftersale.policy.rag.application.EmbeddingClient;
import io.github.tatame.aftersale.policy.rag.application.EmbeddingProviderException;
import io.github.tatame.aftersale.policy.rag.application.EmbeddingRequest;
import io.github.tatame.aftersale.policy.rag.application.EmbeddingResponse;
import io.github.tatame.aftersale.policy.rag.application.FakeEmbeddingClient;
import io.github.tatame.aftersale.policy.rag.application.RagPolicySearchApplicationService;
import io.github.tatame.aftersale.policy.rag.domain.PolicyChunk;
import io.github.tatame.aftersale.policy.rag.domain.PolicyDocument;
import io.github.tatame.aftersale.policy.rag.domain.PolicyEmbedding;
import io.github.tatame.aftersale.policy.rag.domain.PolicyVectorRepository;
import io.github.tatame.aftersale.policy.rag.domain.VectorSearchQuery;
import io.github.tatame.aftersale.policy.rag.domain.VectorSearchResult;
import io.github.tatame.aftersale.policy.rag.infrastructure.memory.InMemoryPolicyVectorRepository;
import io.github.tatame.aftersale.policy.rag.infrastructure.pgvector.PgVectorProperties;
import java.util.List;
import java.util.Optional;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class RagHealthIndicatorTest {

    @Test
    void searchHealthReportsSupportedModesWithoutExecutingSearch() {
        RagSearchHealthIndicator indicator = new RagSearchHealthIndicator(
                provider(new RagPolicySearchApplicationService(
                        new PolicyApplicationService(new InMemoryPolicyRepository()),
                        List.of(),
                        List.of(),
                        new ApplicationMetricsRecorder(new SimpleMeterRegistry()))),
                detailedProperties());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("searchServiceAvailable", true);
        assertThat(health.getDetails().get("supportedRetrievalModes").toString())
                .contains("KEYWORD", "VECTOR", "HYBRID");
        assertThat(health.getDetails()).containsEntry("liveSearchExecuted", false);
    }

    @Test
    void vectorStoreHealthReportsDisabledProviderAsOfflineReadyWithoutSearch() {
        CountingVectorRepository repository = new CountingVectorRepository(new InMemoryPolicyVectorRepository());
        RagVectorStoreHealthIndicator indicator = new RagVectorStoreHealthIndicator(
                "none",
                provider(List.of(repository)),
                provider(null),
                detailedProperties());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("disabled", true);
        assertThat(health.getDetails()).containsEntry("databaseConnectionAttempted", false);
        assertThat(health.getDetails()).containsEntry("vectorSearchExecuted", false);
        assertThat(repository.searchCalls()).isZero();
    }

    @Test
    void vectorStoreHealthReportsFakeProviderUpWhenRepositoryBeanExists() {
        RagVectorStoreHealthIndicator indicator = new RagVectorStoreHealthIndicator(
                "fake",
                provider(List.of(new InMemoryPolicyVectorRepository())),
                provider(null),
                detailedProperties());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("provider", "fake");
        assertThat(health.getDetails()).containsEntry("repositoryBeans", 1);
    }

    @Test
    void vectorStoreHealthReportsPgvectorMissingConfigDownAndSanitized() {
        PgVectorProperties pgVector = new PgVectorProperties(
                true,
                "jdbc:postgresql://user:secret@localhost:5432/ragdb?password=prod",
                "",
                "top-secret",
                "public",
                false,
                1536);
        RagVectorStoreHealthIndicator indicator = new RagVectorStoreHealthIndicator(
                "pgvector",
                provider(List.of()),
                provider(pgVector),
                detailedProperties());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        String details = health.getDetails().toString();
        assertThat(details).contains("passwordConfigured=true");
        assertThat(details).doesNotContain("top-secret");
        assertThat(details).doesNotContain("secret@");
        assertThat(details).doesNotContain("password=prod");
        assertThat(details).contains("databaseConnectionAttempted=false");
    }

    @Test
    void embeddingHealthReportsDisabledProviderWithoutCallingClient() {
        CountingEmbeddingClient embeddingClient = new CountingEmbeddingClient(new FakeEmbeddingClient());
        RagEmbeddingHealthIndicator indicator = new RagEmbeddingHealthIndicator(
                provider(List.of(embeddingClient)),
                provider(new SpringAiProviderProperties()),
                detailedProperties());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("provider", "disabled");
        assertThat(health.getDetails()).containsEntry("embeddingCallExecuted", false);
        assertThat(health.getDetails()).containsEntry("springAiCallExecuted", false);
        assertThat(embeddingClient.calls()).isZero();
    }

    @Test
    void embeddingHealthReportsSpringAiMissingConfigDownAndDoesNotExposeSecret() {
        SpringAiProviderProperties springAi = new SpringAiProviderProperties();
        springAi.setEnabled(true);
        springAi.setEmbeddingEnabled(true);
        springAi.setProviderType("openai");
        springAi.setApiKey("");
        springAi.setEndpointHost("api.example.test?token=secret-token");
        RagEmbeddingHealthIndicator indicator = new RagEmbeddingHealthIndicator(
                provider(List.of()),
                provider(springAi),
                detailedProperties());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        String details = health.getDetails().toString();
        assertThat(details).contains("apiKeyConfigured=false");
        assertThat(details).doesNotContain("secret-token");
        assertThat(details).contains("embeddingCallExecuted=false");
    }

    @Test
    void ingestionHealthReportsContractsWithoutExecutingPipeline() {
        RagIngestionHealthIndicator indicator = new RagIngestionHealthIndicator(
                provider(null),
                provider(null),
                provider(null),
                provider(null),
                detailedProperties());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("chunkingContractAvailable", true);
        assertThat(health.getDetails()).containsEntry("chunkingExecuted", false);
        assertThat(health.getDetails()).containsEntry("embeddingCallExecuted", false);
        assertThat(health.getDetails()).containsEntry("repositoryWriteExecuted", false);
    }

    private static RagHealthProperties detailedProperties() {
        RagHealthProperties properties = new RagHealthProperties();
        properties.setIncludeDetails(true);
        return properties;
    }

    private static <T> ObjectProvider<T> provider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }

            @Override
            public T getObject() {
                return value;
            }
        };
    }

    private static final class CountingEmbeddingClient implements EmbeddingClient {

        private final EmbeddingClient delegate;
        private int calls;

        private CountingEmbeddingClient(EmbeddingClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public EmbeddingResponse embed(EmbeddingRequest request) throws EmbeddingProviderException {
            calls++;
            return delegate.embed(request);
        }

        private int calls() {
            return calls;
        }
    }

    private static final class CountingVectorRepository implements PolicyVectorRepository {

        private final PolicyVectorRepository delegate;
        private int searchCalls;

        private CountingVectorRepository(PolicyVectorRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public PolicyDocument saveDocument(PolicyDocument document) {
            return delegate.saveDocument(document);
        }

        @Override
        public PolicyChunk saveChunk(PolicyChunk chunk) {
            return delegate.saveChunk(chunk);
        }

        @Override
        public PolicyEmbedding saveEmbedding(PolicyEmbedding embedding) {
            return delegate.saveEmbedding(embedding);
        }

        @Override
        public Optional<PolicyDocument> findDocumentById(String documentId) {
            return delegate.findDocumentById(documentId);
        }

        @Override
        public Optional<PolicyChunk> findChunkById(String chunkId) {
            return delegate.findChunkById(chunkId);
        }

        @Override
        public List<PolicyChunk> findChunksByDocumentId(String documentId) {
            return delegate.findChunksByDocumentId(documentId);
        }

        @Override
        public Optional<PolicyEmbedding> findEmbeddingByChunkIdAndModel(String chunkId, String embeddingModel) {
            return delegate.findEmbeddingByChunkIdAndModel(chunkId, embeddingModel);
        }

        @Override
        public VectorSearchResult search(VectorSearchQuery query) {
            searchCalls++;
            return delegate.search(query);
        }

        private int searchCalls() {
            return searchCalls;
        }
    }
}
