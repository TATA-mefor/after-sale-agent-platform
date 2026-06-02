package io.github.tatame.aftersale.policy.rag.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.tatame.aftersale.policy.rag.ingestion.domain.PolicyIngestionChunk;
import io.github.tatame.aftersale.policy.rag.ingestion.domain.PolicyIngestionChunkStatus;
import io.github.tatame.aftersale.policy.rag.ingestion.domain.PolicyIngestionDocument;
import io.github.tatame.aftersale.policy.rag.ingestion.domain.PolicyIngestionSourceType;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PolicyContentChecksumServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-27T00:00:00Z");

    private final PolicyContentChecksumService checksumService = new PolicyContentChecksumService();

    @Test
    void sameNormalizedContentProducesSameChecksum() {
        PolicyChecksum first = checksumService.checksumContent("  return policy\r\nline two  ");
        PolicyChecksum second = checksumService.checksumContent("return policy\nline two");

        assertThat(first.algorithm()).isEqualTo(ChecksumAlgorithm.SHA_256);
        assertThat(first).isEqualTo(second);
        assertThat(first.value()).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void differentContentProducesDifferentChecksum() {
        PolicyChecksum first = checksumService.checksumContent("return policy");
        PolicyChecksum second = checksumService.checksumContent("exchange policy");

        assertThat(first.value()).isNotEqualTo(second.value());
    }

    @Test
    void documentAndChunkChecksumCanBeGenerated() {
        PolicyIngestionDocument document = sampleDocument("Document checksum text.");
        PolicyIngestionChunk chunk = sampleChunk("Chunk checksum text.");

        assertThat(checksumService.checksumDocument(document).value()).hasSize(64);
        assertThat(checksumService.checksumChunk(chunk).value()).hasSize(64);
    }

    @Test
    void blankContentFailsClearly() {
        assertThatThrownBy(() -> checksumService.checksumContent("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("content must not be blank");
    }

    private static PolicyIngestionDocument sampleDocument(String rawText) {
        return new PolicyIngestionDocument(
                "doc-1",
                "run-1",
                "Return Policy",
                "RETURN",
                "electronics",
                "v1",
                PolicyIngestionSourceType.LOCAL_MARKDOWN,
                "policy://return.md",
                rawText,
                null,
                LocalDate.parse("2026-01-01"),
                null,
                "{}",
                NOW);
    }

    private static PolicyIngestionChunk sampleChunk(String content) {
        return new PolicyIngestionChunk(
                "chunk-1",
                "run-1",
                "doc-1",
                0,
                content,
                8,
                null,
                "{}",
                PolicyIngestionChunkStatus.CHUNKED,
                null,
                NOW);
    }
}
