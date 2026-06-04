package io.github.tatame.aftersale.policy.rag.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.tatame.aftersale.common.observability.metrics.ApplicationMetricsRecorder;
import io.github.tatame.aftersale.policy.application.PolicyApplicationService;
import io.github.tatame.aftersale.policy.infrastructure.repository.InMemoryPolicyRepository;
import io.github.tatame.aftersale.policy.rag.domain.PolicyChunk;
import io.github.tatame.aftersale.policy.rag.domain.PolicyDocument;
import io.github.tatame.aftersale.policy.rag.domain.PolicyDocumentSourceType;
import io.github.tatame.aftersale.policy.rag.domain.PolicyEmbedding;
import io.github.tatame.aftersale.policy.rag.domain.PolicyVectorRepository;
import io.github.tatame.aftersale.policy.rag.search.RagPolicyEvidenceSource;
import io.github.tatame.aftersale.policy.rag.search.RagPolicySearchQuery;
import io.github.tatame.aftersale.policy.rag.search.RagPolicySearchResult;
import io.github.tatame.aftersale.policy.rag.search.RetrievalMode;
import io.github.tatame.aftersale.policy.rag.infrastructure.memory.InMemoryPolicyVectorRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RagPolicySearchApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-27T00:00:00Z");

    @Test
    void keywordModeUsesExistingPolicyRetrievalWithoutVectorDependencies() {
        CountingEmbeddingClient embeddingClient = new CountingEmbeddingClient(new FakeEmbeddingClient(4));
        CountingVectorRepository vectorRepository = new CountingVectorRepository(seedVectorRepository(embeddingClient));
        embeddingClient.reset();
        RagPolicySearchApplicationService service = newService(List.of(embeddingClient), List.of(vectorRepository));

        RagPolicySearchResult result = service.search(query("商品有质量问题", RetrievalMode.KEYWORD));

        assertThat(result.retrievalMode()).isEqualTo(RetrievalMode.KEYWORD);
        assertThat(result.totalKeywordMatches()).isPositive();
        assertThat(result.evidences()).extracting(evidence -> evidence.policyId())
                .contains("POL-QUALITY-RETURN-EXCHANGE");
        assertThat(embeddingClient.calls()).isZero();
        assertThat(vectorRepository.searchCalls()).isZero();
    }

    @Test
    void vectorModeUsesFakeEmbeddingAndInMemoryVectorRepository() {
        CountingEmbeddingClient embeddingClient = new CountingEmbeddingClient(new FakeEmbeddingClient(4));
        CountingVectorRepository vectorRepository = new CountingVectorRepository(seedVectorRepository(embeddingClient));
        RagPolicySearchApplicationService service = newService(List.of(embeddingClient), List.of(vectorRepository));

        RagPolicySearchResult result = service.search(query("质量问题退换货", RetrievalMode.VECTOR));

        assertThat(result.retrievalMode()).isEqualTo(RetrievalMode.VECTOR);
        assertThat(result.evidences()).isNotEmpty();
        assertThat(result.evidences().get(0).chunkId()).isNotBlank();
        assertThat(result.evidences().get(0).documentId()).isNotBlank();
        assertThat(result.evidences().get(0).source()).isEqualTo(RagPolicyEvidenceSource.VECTOR_CHUNK);
        assertThat(result.evidences().get(0).vectorScore()).isNotNull();
        assertThat(embeddingClient.calls()).isGreaterThan(0);
        assertThat(vectorRepository.searchCalls()).isEqualTo(1);
    }

    @Test
    void vectorModeReturnsClearFailureResultWhenRuntimeIsUnavailable() {
        RagPolicySearchApplicationService service = newService(List.of(), List.of());

        RagPolicySearchResult result = service.search(query("质量问题退换货", RetrievalMode.VECTOR));

        assertThat(result.retrievalMode()).isEqualTo(RetrievalMode.VECTOR);
        assertThat(result.evidences()).isEmpty();
        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.message()).contains("Vector policy search is unavailable");
    }

    @Test
    void hybridModeMergesKeywordAndVectorEvidence() {
        CountingEmbeddingClient embeddingClient = new CountingEmbeddingClient(new FakeEmbeddingClient(4));
        CountingVectorRepository vectorRepository = new CountingVectorRepository(seedVectorRepository(embeddingClient));
        RagPolicySearchApplicationService service = newService(List.of(embeddingClient), List.of(vectorRepository));

        RagPolicySearchResult result = service.search(query("质量问题退换货", RetrievalMode.HYBRID));

        assertThat(result.retrievalMode()).isEqualTo(RetrievalMode.HYBRID);
        assertThat(result.evidences()).isNotEmpty();
        assertThat(result.totalKeywordMatches()).isPositive();
        assertThat(result.totalVectorMatches()).isPositive();
        assertThat(result.evidences()).allSatisfy(evidence -> {
            assertThat(evidence.retrievalMode()).isEqualTo(RetrievalMode.HYBRID);
            assertThat(evidence.source()).isEqualTo(RagPolicyEvidenceSource.MERGED_HYBRID);
        });
    }

    @Test
    void hybridModeFallsBackToKeywordWhenVectorFailsAndSanitizesMessage() {
        EmbeddingClient failingClient = request -> {
            throw new EmbeddingProviderException(
                    "provider failed apiKey=sk-secret password=prod token=abc D:\\secret\\policy.txt");
        };
        RagPolicySearchApplicationService service = newService(
                List.of(failingClient),
                List.of(new InMemoryPolicyVectorRepository()));

        RagPolicySearchResult result = service.search(query("商品有质量问题", RetrievalMode.HYBRID));

        assertThat(result.retrievalMode()).isEqualTo(RetrievalMode.HYBRID);
        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.evidences()).isNotEmpty();
        assertThat(result.message()).contains("keyword evidence only");
        assertThat(result.message()).doesNotContain("sk-secret");
        assertThat(result.message()).doesNotContain("password=prod");
        assertThat(result.message()).doesNotContain("token=abc");
        assertThat(result.message()).doesNotContain("D:\\");
    }

    private static RagPolicySearchApplicationService newService(
            List<EmbeddingClient> embeddingClients,
            List<PolicyVectorRepository> vectorRepositories) {
        return new RagPolicySearchApplicationService(
                new PolicyApplicationService(new InMemoryPolicyRepository()),
                embeddingClients,
                vectorRepositories,
                new ApplicationMetricsRecorder(new SimpleMeterRegistry()));
    }

    private static RagPolicySearchQuery query(String text, RetrievalMode mode) {
        return new RagPolicySearchQuery(
                text,
                mode,
                RagPolicySearchQuery.DEFAULT_TOP_K,
                null,
                null,
                null,
                null,
                RagPolicySearchApplicationService.DEFAULT_EMBEDDING_MODEL,
                mode != RetrievalMode.VECTOR,
                mode != RetrievalMode.KEYWORD);
    }

    private static PolicyVectorRepository seedVectorRepository(EmbeddingClient embeddingClient) {
        InMemoryPolicyVectorRepository repository = new InMemoryPolicyVectorRepository();
        save(repository, embeddingClient, "doc-quality", "Quality Return Policy", "质量问题退换货规则",
                "通用商品", "chunk-quality", "商品存在质量问题时，可申请退货、退款或换货。");
        save(repository, embeddingClient, "doc-logistics", "Logistics Policy", "已签收未收到物流争议规则",
                "通用商品", "chunk-logistics", "物流显示签收但用户未收到货时，应核验签收凭证。");
        return repository;
    }

    private static void save(
            InMemoryPolicyVectorRepository repository,
            EmbeddingClient embeddingClient,
            String documentId,
            String title,
            String category,
            String productType,
            String chunkId,
            String content) {
        repository.saveDocument(new PolicyDocument(
                documentId,
                title,
                category,
                productType,
                "v1",
                PolicyDocumentSourceType.MARKDOWN,
                "policy://" + documentId,
                "checksum-" + documentId,
                LocalDate.parse("2026-01-01"),
                null,
                NOW,
                NOW));
        repository.saveChunk(new PolicyChunk(chunkId, documentId, 0, content, 20, "{}", NOW));
        EmbeddingResponse embedding = embeddingClient.embed(new EmbeddingRequest(
                RagPolicySearchApplicationService.DEFAULT_EMBEDDING_MODEL,
                content));
        repository.saveEmbedding(new PolicyEmbedding(
                "embedding-" + chunkId,
                chunkId,
                embedding.model(),
                embedding.dimension(),
                embedding.vector(),
                NOW));
    }

    private static final class CountingEmbeddingClient implements EmbeddingClient {

        private final EmbeddingClient delegate;
        private int calls;

        private CountingEmbeddingClient(EmbeddingClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public EmbeddingResponse embed(EmbeddingRequest request) {
            calls++;
            return delegate.embed(request);
        }

        private int calls() {
            return calls;
        }

        private void reset() {
            calls = 0;
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
        public java.util.Optional<PolicyDocument> findDocumentById(String documentId) {
            return delegate.findDocumentById(documentId);
        }

        @Override
        public java.util.Optional<PolicyChunk> findChunkById(String chunkId) {
            return delegate.findChunkById(chunkId);
        }

        @Override
        public List<PolicyChunk> findChunksByDocumentId(String documentId) {
            return delegate.findChunksByDocumentId(documentId);
        }

        @Override
        public java.util.Optional<PolicyEmbedding> findEmbeddingByChunkIdAndModel(
                String chunkId,
                String embeddingModel) {
            return delegate.findEmbeddingByChunkIdAndModel(chunkId, embeddingModel);
        }

        @Override
        public io.github.tatame.aftersale.policy.rag.domain.VectorSearchResult search(
                io.github.tatame.aftersale.policy.rag.domain.VectorSearchQuery query) {
            searchCalls++;
            return delegate.search(query);
        }

        private int searchCalls() {
            return searchCalls;
        }
    }
}
