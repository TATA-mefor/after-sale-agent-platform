package com.example.aftersale.policy.rag.ingestion.infrastructure.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionChunk;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionDocument;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionError;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionRepository;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionRun;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionSource;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionSourceType;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionStatus;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionChunkStatus;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InMemoryPolicyIngestionRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-05-27T00:00:00Z");

    @Test
    void savesFindsAndUpdatesRuns() {
        PolicyIngestionRepository repository = new InMemoryPolicyIngestionRepository();
        PolicyIngestionRun run = sampleRun();

        repository.saveRun(run);
        PolicyIngestionRun running = run.transitionTo(PolicyIngestionStatus.RUNNING, NOW.plusSeconds(1), null);
        repository.updateRun(running);

        assertThat(repository.findRunById("run-1")).contains(running);
    }

    @Test
    void savesAndFindsDocumentsChunksAndErrorsByRunAndDocument() {
        PolicyIngestionRepository repository = seededRepository();

        assertThat(repository.findDocumentsByRunId("run-1"))
                .extracting(PolicyIngestionDocument::ingestionDocumentId)
                .containsExactly("doc-1");
        assertThat(repository.findChunksByRunId("run-1"))
                .extracting(PolicyIngestionChunk::ingestionChunkId)
                .containsExactly("chunk-1");
        assertThat(repository.findChunksByDocumentId("doc-1"))
                .extracting(PolicyIngestionChunk::ingestionChunkId)
                .containsExactly("chunk-1");
        assertThat(repository.findErrorsByRunId("run-1"))
                .extracting(PolicyIngestionError::errorId)
                .containsExactly("error-1");
    }

    @Test
    void duplicateRunDocumentChunkAndErrorIdsAreRejected() {
        InMemoryPolicyIngestionRepository repository = new InMemoryPolicyIngestionRepository();
        PolicyIngestionRun run = sampleRun();
        PolicyIngestionDocument document = sampleDocument();
        PolicyIngestionChunk chunk = sampleChunk();
        PolicyIngestionError error = sampleError();

        repository.saveRun(run);
        assertThatThrownBy(() -> repository.saveRun(run))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate ingestion run");

        repository.saveDocument(document);
        assertThatThrownBy(() -> repository.saveDocument(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate ingestion document");

        repository.saveChunk(chunk);
        assertThatThrownBy(() -> repository.saveChunk(chunk))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate ingestion chunk");

        repository.saveError(error);
        assertThatThrownBy(() -> repository.saveError(error))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate ingestion error");
    }

    @Test
    void childRecordsRequireSavedParents() {
        InMemoryPolicyIngestionRepository repository = new InMemoryPolicyIngestionRepository();

        assertThatThrownBy(() -> repository.updateRun(sampleRun()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("run must be saved");
        assertThatThrownBy(() -> repository.saveDocument(sampleDocument()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("run must be saved");

        repository.saveRun(sampleRun());
        assertThatThrownBy(() -> repository.saveChunk(sampleChunk()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("document must be saved");
    }

    @Test
    void returnedCollectionsAreImmutableSnapshots() {
        PolicyIngestionRepository repository = seededRepository();

        assertThatThrownBy(() -> repository.findDocumentsByRunId("run-1").clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> repository.findChunksByRunId("run-1").clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> repository.findErrorsByRunId("run-1").clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static PolicyIngestionRepository seededRepository() {
        InMemoryPolicyIngestionRepository repository = new InMemoryPolicyIngestionRepository();
        repository.saveRun(sampleRun());
        repository.saveDocument(sampleDocument());
        repository.saveChunk(sampleChunk());
        repository.saveError(sampleError());
        return repository;
    }

    private static PolicyIngestionRun sampleRun() {
        return new PolicyIngestionRun(
                "run-1",
                new PolicyIngestionSource(
                        "source-1",
                        PolicyIngestionSourceType.LOCAL_MARKDOWN,
                        "policy://return.md",
                        "Return policy markdown",
                        "checksum-source",
                        null),
                PolicyIngestionStatus.CREATED,
                0,
                0,
                0,
                0,
                null,
                NOW,
                null,
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
                "Policy text for ingestion only.",
                "checksum-doc",
                LocalDate.parse("2026-01-01"),
                null,
                null,
                NOW);
    }

    private static PolicyIngestionChunk sampleChunk() {
        return new PolicyIngestionChunk(
                "chunk-1",
                "run-1",
                "doc-1",
                0,
                "Policy chunk evidence text.",
                12,
                "checksum-chunk",
                null,
                PolicyIngestionChunkStatus.CHUNKED,
                null,
                NOW);
    }

    private static PolicyIngestionError sampleError() {
        return new PolicyIngestionError(
                "error-1",
                "run-1",
                "doc-1",
                "chunk-1",
                "PARSE_ERROR",
                "Failed to parse policy document",
                "Missing heading",
                NOW);
    }
}
