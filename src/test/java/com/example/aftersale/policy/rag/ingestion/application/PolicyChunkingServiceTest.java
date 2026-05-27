package com.example.aftersale.policy.rag.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionChunk;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionDocument;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionSourceType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PolicyChunkingServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-27T00:00:00Z");
    private static final PolicyChunkingOptions SMALL_OPTIONS = new PolicyChunkingOptions(
            40,
            5,
            10,
            4,
            true,
            12,
            PolicyChunkingStrategy.DETERMINISTIC_CHARACTER_WINDOW);

    private final PolicyChunkingService chunkingService = new PolicyChunkingService();

    @Test
    void optionsValidateBoundaries() {
        assertThatThrownBy(() -> new PolicyChunkingOptions(0, 0, 1, 4, true, 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxChunkChars must be positive");
        assertThatThrownBy(() -> new PolicyChunkingOptions(10, 10, 1, 4, true, 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlapChars must be less than maxChunkChars");
        assertThatThrownBy(() -> new PolicyChunkingOptions(10, 0, 0, 4, true, 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxChunksPerDocument must be positive");
        assertThatThrownBy(() -> new PolicyChunkingOptions(10, 0, 1, 0, true, 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tokenEstimateDivisor must be positive");
    }

    @Test
    void blankRawTextFailsBeforeChunkingWithoutEchoingText() {
        assertThatThrownBy(() -> sampleDocument("       "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rawText must not be blank")
                .hasMessageNotContaining("       ");
    }

    @Test
    void shortTextGeneratesOneChunkWithDeterministicIndexAndChecksum() {
        PolicyChunkingResult result = chunkingService.chunk(
                sampleDocument("Seven day return policy."),
                SMALL_OPTIONS,
                NOW);

        assertThat(result.chunks()).hasSize(1);
        PolicyIngestionChunk chunk = result.chunks().get(0);
        assertThat(chunk.ingestionChunkId()).isEqualTo("doc-1-chunk-0");
        assertThat(chunk.chunkIndex()).isZero();
        assertThat(chunk.content()).isEqualTo("Seven day return policy.");
        assertThat(chunk.tokenEstimate()).isEqualTo(6);
        assertThat(chunk.checksum()).hasSize(64);
    }

    @Test
    void longTextGeneratesMultipleChunksWithOverlap() {
        String text = "A".repeat(35) + "B".repeat(35) + "C".repeat(35);

        List<PolicyIngestionChunk> chunks = chunkingService.chunk(sampleDocument(text), SMALL_OPTIONS, NOW)
                .chunks();

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).extracting(PolicyIngestionChunk::chunkIndex).containsExactly(0, 1, 2);
        assertThat(chunks.get(1).content()).startsWith(chunks.get(0).content()
                .substring(chunks.get(0).content().length() - SMALL_OPTIONS.overlapChars()));
    }

    @Test
    void paragraphBoundaryIsPreservedWhenAvailable() {
        PolicyChunkingOptions options = new PolicyChunkingOptions(
                45,
                0,
                10,
                4,
                true,
                12,
                PolicyChunkingStrategy.DETERMINISTIC_CHARACTER_WINDOW);
        String text = "First paragraph has policy text.\n\nSecond paragraph has exchange text.\n\nThird paragraph.";

        List<PolicyIngestionChunk> chunks = chunkingService.chunk(sampleDocument(text), options, NOW)
                .chunks();

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0).content()).isEqualTo("First paragraph has policy text.");
        assertThat(chunks.get(1).content()).isEqualTo("Second paragraph has exchange text.");
    }

    @Test
    void maxChunksOverflowFailsClearlyWithoutRawText() {
        PolicyChunkingOptions options = new PolicyChunkingOptions(
                10,
                0,
                1,
                4,
                false,
                1,
                PolicyChunkingStrategy.DETERMINISTIC_CHARACTER_WINDOW);
        String rawText = "sensitive policy text should not be echoed ".repeat(3);

        assertThatThrownBy(() -> chunkingService.chunk(sampleDocument(rawText), options, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maxChunksPerDocument")
                .hasMessageNotContaining("sensitive policy text");
    }

    @Test
    void resultChunkListIsImmutable() {
        PolicyChunkingResult result = chunkingService.chunk(sampleDocument("Short policy text."), SMALL_OPTIONS, NOW);

        assertThatThrownBy(() -> result.chunks().clear())
                .isInstanceOf(UnsupportedOperationException.class);
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
}
