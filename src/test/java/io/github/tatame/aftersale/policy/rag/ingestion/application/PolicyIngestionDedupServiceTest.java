package io.github.tatame.aftersale.policy.rag.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.tatame.aftersale.policy.rag.ingestion.domain.PolicyIngestionChunk;
import io.github.tatame.aftersale.policy.rag.ingestion.domain.PolicyIngestionChunkStatus;
import io.github.tatame.aftersale.policy.rag.ingestion.domain.PolicyIngestionDocument;
import io.github.tatame.aftersale.policy.rag.ingestion.domain.PolicyIngestionRepository;
import io.github.tatame.aftersale.policy.rag.ingestion.domain.PolicyIngestionRun;
import io.github.tatame.aftersale.policy.rag.ingestion.domain.PolicyIngestionSource;
import io.github.tatame.aftersale.policy.rag.ingestion.domain.PolicyIngestionSourceType;
import io.github.tatame.aftersale.policy.rag.ingestion.domain.PolicyIngestionStatus;
import io.github.tatame.aftersale.policy.rag.ingestion.infrastructure.memory.InMemoryPolicyIngestionRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PolicyIngestionDedupServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-27T00:00:00Z");
    private static final PolicyContentChecksumService CHECKSUM_SERVICE = new PolicyContentChecksumService();
    private static final String DOCUMENT_TEXT = "Policy text for ingestion only.";
    private static final String CHUNK_TEXT = "Policy chunk evidence text.";
    private static final String DOCUMENT_CHECKSUM = CHECKSUM_SERVICE.checksumContent(DOCUMENT_TEXT).value();
    private static final String CHUNK_CHECKSUM = CHECKSUM_SERVICE.checksumContent(CHUNK_TEXT).value();

    @Test
    void newDocumentChecksumReturnsNewContent() {
        PolicyIngestionDedupService dedupService = new PolicyIngestionDedupService(seededRepository());

        PolicyDedupDecision decision = dedupService.checkDocumentChecksum("new-doc-checksum");

        assertThat(decision.type()).isEqualTo(PolicyDedupDecisionType.NEW_CONTENT);
        assertThat(decision.duplicate()).isFalse();
        assertThat(decision.reason()).isEqualTo("document checksum is new");
    }

    @Test
    void duplicateDocumentChecksumReturnsDuplicateDocumentWithoutRawText() {
        PolicyIngestionDedupService dedupService = new PolicyIngestionDedupService(seededRepository());

        PolicyDedupDecision decision = dedupService.checkDocumentChecksum(DOCUMENT_CHECKSUM);

        assertThat(decision.type()).isEqualTo(PolicyDedupDecisionType.DUPLICATE_DOCUMENT);
        assertThat(decision.duplicate()).isTrue();
        assertThat(decision.existingDocumentId()).isEqualTo("doc-1");
        assertThat(decision.reason()).doesNotContain("Policy text for ingestion only");
    }

    @Test
    void newChunkChecksumReturnsNewContent() {
        PolicyIngestionDedupService dedupService = new PolicyIngestionDedupService(seededRepository());

        PolicyDedupDecision decision = dedupService.checkChunkChecksum("doc-1", "new-chunk-checksum");

        assertThat(decision.type()).isEqualTo(PolicyDedupDecisionType.NEW_CONTENT);
        assertThat(decision.reason()).isEqualTo("chunk checksum is new for ingestion document");
    }

    @Test
    void duplicateChunkChecksumReturnsDuplicateChunkWithinDocument() {
        PolicyIngestionDedupService dedupService = new PolicyIngestionDedupService(seededRepository());

        PolicyDedupDecision decision = dedupService.checkChunkChecksum("doc-1", CHUNK_CHECKSUM);

        assertThat(decision.type()).isEqualTo(PolicyDedupDecisionType.DUPLICATE_CHUNK);
        assertThat(decision.existingDocumentId()).isEqualTo("doc-1");
        assertThat(decision.existingChunkId()).isEqualTo("chunk-1");
        assertThat(decision.reason()).doesNotContain("Policy chunk evidence text");
    }

    @Test
    void missingChecksumOnRecordsIsCalculatedDeterministically() {
        PolicyIngestionRepository repository = seededRepository();
        PolicyIngestionDedupService dedupService = new PolicyIngestionDedupService(repository);
        PolicyIngestionDocument document = sampleDocument(
                "doc-2",
                "  " + DOCUMENT_TEXT + "\r\n",
                null);
        PolicyIngestionChunk chunk = sampleChunk(
                "chunk-2",
                "doc-1",
                "  " + CHUNK_TEXT + "\r\n",
                null);

        assertThat(dedupService.checkDocument(document).type())
                .isEqualTo(PolicyDedupDecisionType.DUPLICATE_DOCUMENT);
        assertThat(dedupService.checkChunk(chunk).type())
                .isEqualTo(PolicyDedupDecisionType.DUPLICATE_CHUNK);
    }

    private static PolicyIngestionRepository seededRepository() {
        InMemoryPolicyIngestionRepository repository = new InMemoryPolicyIngestionRepository();
        repository.saveRun(sampleRun());
        PolicyIngestionDocument document = sampleDocument(
                "doc-1",
                DOCUMENT_TEXT,
                DOCUMENT_CHECKSUM);
        repository.saveDocument(document);
        PolicyIngestionChunk chunk = sampleChunk(
                "chunk-1",
                "doc-1",
                CHUNK_TEXT,
                CHUNK_CHECKSUM);
        repository.saveChunk(chunk);
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
                        "{}"),
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

    private static PolicyIngestionDocument sampleDocument(
            String documentId,
            String rawText,
            String checksum) {
        return new PolicyIngestionDocument(
                documentId,
                "run-1",
                "Return Policy",
                "RETURN",
                "electronics",
                "v1",
                PolicyIngestionSourceType.LOCAL_MARKDOWN,
                "policy://return.md",
                rawText,
                checksum,
                LocalDate.parse("2026-01-01"),
                null,
                "{}",
                NOW);
    }

    private static PolicyIngestionChunk sampleChunk(
            String chunkId,
            String documentId,
            String content,
            String checksum) {
        return new PolicyIngestionChunk(
                chunkId,
                "run-1",
                documentId,
                0,
                content,
                8,
                checksum,
                "{}",
                PolicyIngestionChunkStatus.CHUNKED,
                null,
                NOW);
    }
}
