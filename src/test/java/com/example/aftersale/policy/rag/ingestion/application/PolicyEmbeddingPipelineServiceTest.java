package com.example.aftersale.policy.rag.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.aftersale.policy.rag.application.EmbeddingClient;
import com.example.aftersale.policy.rag.application.EmbeddingProviderException;
import com.example.aftersale.policy.rag.application.FakeEmbeddingClient;
import com.example.aftersale.policy.rag.domain.PolicyEmbedding;
import com.example.aftersale.policy.rag.domain.PolicyVectorRepository;
import com.example.aftersale.policy.rag.domain.VectorSearchQuery;
import com.example.aftersale.policy.rag.domain.VectorSearchResult;
import com.example.aftersale.policy.rag.infrastructure.memory.InMemoryPolicyVectorRepository;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionChunk;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionChunkStatus;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionDocument;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionRepository;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionRun;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionSource;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionSourceType;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionStatus;
import com.example.aftersale.policy.rag.ingestion.infrastructure.memory.InMemoryPolicyIngestionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class PolicyEmbeddingPipelineServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-27T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String MODEL = "fake-policy-embedding";

    @Test
    void happyPathEmbedsChunksWritesVectorRepositoryAndSupportsDirectVectorSearch() {
        PolicyIngestionRepository ingestionRepository = seededIngestionRepository();
        PolicyVectorRepository vectorRepository = new InMemoryPolicyVectorRepository();
        PolicyEmbeddingPipelineService service = new PolicyEmbeddingPipelineService(
                ingestionRepository,
                vectorRepository,
                new FakeEmbeddingClient(3),
                CLOCK);

        PolicyEmbeddingPipelineResult result = service.embedRun(
                "run-1",
                new PolicyEmbeddingPipelineOptions(MODEL, 3, true, true, 10, 2));

        assertThat(result.status()).isEqualTo(PolicyIngestionStatus.COMPLETED);
        assertThat(result.processedChunks()).isEqualTo(2);
        assertThat(result.embeddedChunks()).isEqualTo(2);
        assertThat(result.savedDocuments()).isEqualTo(1);
        assertThat(result.savedVectorChunks()).isEqualTo(2);
        assertThat(result.savedEmbeddings()).isEqualTo(2);
        assertThat(result.failures()).isEmpty();
        assertThat(ingestionRepository.findRunById("run-1"))
                .map(PolicyIngestionRun::status)
                .contains(PolicyIngestionStatus.COMPLETED);
        assertThat(vectorRepository.findDocumentById("doc-1")).isPresent();
        assertThat(vectorRepository.findChunkById("chunk-1")).isPresent();
        assertThat(vectorRepository.findEmbeddingByChunkIdAndModel("chunk-1", MODEL)).isPresent();

        List<Double> queryVector = vectorRepository.findEmbeddingByChunkIdAndModel("chunk-1", MODEL)
                .map(PolicyEmbedding::vector)
                .orElseThrow();
        VectorSearchResult searchResult = vectorRepository.search(new VectorSearchQuery(
                "return policy",
                queryVector,
                1,
                null,
                "RETURN",
                "electronics",
                LocalDate.parse("2026-05-27"),
                MODEL));

        assertThat(searchResult.matches()).singleElement()
                .satisfies(match -> {
                    assertThat(match.documentId()).isEqualTo("doc-1");
                    assertThat(match.chunkId()).isEqualTo("chunk-1");
                    assertThat(match.embeddingModel()).isEqualTo(MODEL);
                });
    }

    @Test
    void dimensionMismatchFailsWhenStrictAndSkipsWhenAllowed() {
        PolicyEmbeddingPipelineResult strictResult = newService(
                seededIngestionRepository(),
                new InMemoryPolicyVectorRepository(),
                new FakeEmbeddingClient(3))
                .embedRun("run-1", new PolicyEmbeddingPipelineOptions(MODEL, 4, true, true, 10, 2));

        assertThat(strictResult.status()).isEqualTo(PolicyIngestionStatus.FAILED);
        assertThat(strictResult.failedChunks()).isEqualTo(2);
        assertThat(strictResult.savedEmbeddings()).isZero();
        assertThat(strictResult.failures()).extracting(PolicyEmbeddingPipelineFailure::errorCode)
                .containsOnly("DIMENSION_MISMATCH");

        PolicyEmbeddingPipelineResult lenientResult = newService(
                seededIngestionRepository(),
                new InMemoryPolicyVectorRepository(),
                new FakeEmbeddingClient(3))
                .embedRun("run-1", new PolicyEmbeddingPipelineOptions(MODEL, 4, false, true, 10, 2));

        assertThat(lenientResult.status()).isEqualTo(PolicyIngestionStatus.COMPLETED);
        assertThat(lenientResult.skippedChunks()).isEqualTo(2);
        assertThat(lenientResult.savedEmbeddings()).isZero();
        assertThat(lenientResult.failures()).isEmpty();
    }

    @Test
    void duplicateEmbeddingCanBeSkippedOrTreatedAsFailure() {
        PolicyIngestionRepository skipIngestionRepository = seededIngestionRepository();
        PolicyVectorRepository skipVectorRepository = new InMemoryPolicyVectorRepository();
        newService(skipIngestionRepository, skipVectorRepository, new FakeEmbeddingClient(3))
                .embedRun("run-1", new PolicyEmbeddingPipelineOptions(MODEL, 3, true, true, 10, 2));

        PolicyEmbeddingPipelineResult skipped = newService(
                seededIngestionRepository(),
                skipVectorRepository,
                new FakeEmbeddingClient(3))
                .embedRun("run-1", new PolicyEmbeddingPipelineOptions(MODEL, 3, true, true, 10, 2));

        assertThat(skipped.status()).isEqualTo(PolicyIngestionStatus.COMPLETED);
        assertThat(skipped.skippedChunks()).isEqualTo(2);
        assertThat(skipped.savedEmbeddings()).isZero();

        PolicyIngestionRepository failIngestionRepository = seededIngestionRepository();
        PolicyVectorRepository failVectorRepository = new InMemoryPolicyVectorRepository();
        newService(failIngestionRepository, failVectorRepository, new FakeEmbeddingClient(3))
                .embedRun("run-1", new PolicyEmbeddingPipelineOptions(MODEL, 3, true, true, 10, 2));

        PolicyEmbeddingPipelineResult failed = newService(
                seededIngestionRepository(),
                failVectorRepository,
                new FakeEmbeddingClient(3))
                .embedRun("run-1", new PolicyEmbeddingPipelineOptions(MODEL, 3, true, false, 10, 2));

        assertThat(failed.status()).isEqualTo(PolicyIngestionStatus.FAILED);
        assertThat(failed.failedChunks()).isEqualTo(2);
        assertThat(failed.failures()).extracting(PolicyEmbeddingPipelineFailure::errorCode)
                .containsOnly("DUPLICATE_EMBEDDING");
    }

    @Test
    void partialFailureKeepsSuccessfulEmbeddingsAndSanitizesFailure() {
        String sensitiveContent = "Sensitive return policy chunk should never be echoed in a failure.";
        PolicyIngestionRepository ingestionRepository = seededIngestionRepository(sensitiveContent);
        PolicyVectorRepository vectorRepository = new InMemoryPolicyVectorRepository();
        EmbeddingClient client = request -> {
            if (request.text().contains("Sensitive return")) {
                throw new EmbeddingProviderException("api_key=sk-test password=secret token=abc "
                        + "prompt=full prompt C:\\private\\policy.md " + request.text());
            }
            return new FakeEmbeddingClient(3).embed(request);
        };

        PolicyEmbeddingPipelineResult result = newService(ingestionRepository, vectorRepository, client)
                .embedRun("run-1", new PolicyEmbeddingPipelineOptions(MODEL, 3, true, true, 10, 2));

        assertThat(result.status()).isEqualTo(PolicyIngestionStatus.PARTIALLY_FAILED);
        assertThat(result.embeddedChunks()).isEqualTo(1);
        assertThat(result.failedChunks()).isEqualTo(1);
        assertThat(vectorRepository.findEmbeddingByChunkIdAndModel("chunk-2", MODEL)).isPresent();
        assertThat(result.failures()).singleElement().satisfies(failure -> {
            assertThat(failure.message()).doesNotContain("sk-test", "secret", "abc");
            assertThat(failure.sanitizedDetails()).doesNotContain("sk-test", "secret", "abc");
            assertThat(failure.sanitizedDetails()).doesNotContain("C:\\private");
            assertThat(failure.sanitizedDetails()).doesNotContain(sensitiveContent);
            assertThat(failure.sanitizedDetails()).doesNotContain("full prompt");
        });
    }

    @Test
    void allFailuresMarkRunFailedAndDoNotWriteEmbeddings() {
        PolicyIngestionRepository ingestionRepository = seededIngestionRepository();
        PolicyVectorRepository vectorRepository = new InMemoryPolicyVectorRepository();
        EmbeddingClient failingClient = request -> {
            throw new EmbeddingProviderException("embedding unavailable");
        };

        PolicyEmbeddingPipelineResult result = newService(ingestionRepository, vectorRepository, failingClient)
                .embedRun("run-1", PolicyEmbeddingPipelineOptions.defaults());

        assertThat(result.status()).isEqualTo(PolicyIngestionStatus.FAILED);
        assertThat(result.failedChunks()).isEqualTo(2);
        assertThat(result.savedEmbeddings()).isZero();
        assertThat(vectorRepository.findEmbeddingByChunkIdAndModel("chunk-1", MODEL)).isEmpty();
        assertThat(ingestionRepository.findRunById("run-1"))
                .map(PolicyIngestionRun::status)
                .contains(PolicyIngestionStatus.FAILED);
    }

    @Test
    void blankChunkContentIsRejectedBeforeEmbeddingPipeline() {
        assertThatThrownBy(() -> sampleChunk("chunk-blank", 0, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("content must not be blank");
    }

    @Test
    void rejectsRunsThatAreNotReadyForEmbeddingAndProtectsChunkLimit() {
        PolicyIngestionRepository createdRepository = new InMemoryPolicyIngestionRepository();
        createdRepository.saveRun(sampleRun(PolicyIngestionStatus.CREATED));

        assertThatThrownBy(() -> newService(
                createdRepository,
                new InMemoryPolicyVectorRepository(),
                new FakeEmbeddingClient(3))
                .embedRun("run-1", PolicyEmbeddingPipelineOptions.defaults()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CHUNKED or EMBEDDING");

        assertThatThrownBy(() -> newService(
                seededIngestionRepository(),
                new InMemoryPolicyVectorRepository(),
                new FakeEmbeddingClient(3))
                .embedRun("run-1", new PolicyEmbeddingPipelineOptions(MODEL, 3, true, true, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxChunksPerRun");
    }

    private static PolicyEmbeddingPipelineService newService(
            PolicyIngestionRepository ingestionRepository,
            PolicyVectorRepository vectorRepository,
            EmbeddingClient embeddingClient) {
        return new PolicyEmbeddingPipelineService(ingestionRepository, vectorRepository, embeddingClient, CLOCK);
    }

    private static PolicyIngestionRepository seededIngestionRepository() {
        return seededIngestionRepository("Return quality policy evidence for fake embedding.");
    }

    private static PolicyIngestionRepository seededIngestionRepository(String firstChunkContent) {
        PolicyIngestionRepository repository = new InMemoryPolicyIngestionRepository();
        repository.saveRun(sampleRun(PolicyIngestionStatus.CHUNKED));
        repository.saveDocument(sampleDocument());
        repository.saveChunk(sampleChunk("chunk-1", 0, firstChunkContent));
        repository.saveChunk(sampleChunk("chunk-2", 1, "Exchange policy evidence for fake embedding."));
        return repository;
    }

    private static PolicyIngestionRun sampleRun(PolicyIngestionStatus status) {
        Instant finishedAt = status.isTerminal() ? NOW : null;
        return new PolicyIngestionRun(
                "run-1",
                new PolicyIngestionSource(
                        "source-1",
                        PolicyIngestionSourceType.LOCAL_MARKDOWN,
                        "policy://return.md",
                        "Return policy markdown",
                        "checksum-source",
                        null),
                status,
                1,
                2,
                0,
                0,
                null,
                NOW,
                finishedAt,
                NOW,
                NOW);
    }

    private static PolicyIngestionDocument sampleDocument() {
        return new PolicyIngestionDocument(
                "doc-1",
                "run-1",
                "Return Policy",
                "RETURN",
                "electronics",
                "v1",
                PolicyIngestionSourceType.LOCAL_MARKDOWN,
                "policy://return.md",
                "Raw ingestion text used only for pipeline setup.",
                "checksum-doc",
                LocalDate.parse("2026-01-01"),
                null,
                "{}",
                NOW);
    }

    private static PolicyIngestionChunk sampleChunk(String chunkId, int chunkIndex, String content) {
        return new PolicyIngestionChunk(
                chunkId,
                "run-1",
                "doc-1",
                chunkIndex,
                content,
                12,
                "checksum-" + chunkId,
                "{}",
                PolicyIngestionChunkStatus.CHUNKED,
                null,
                NOW);
    }
}
