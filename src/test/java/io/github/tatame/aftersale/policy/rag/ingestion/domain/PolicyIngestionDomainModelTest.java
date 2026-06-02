package io.github.tatame.aftersale.policy.rag.ingestion.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PolicyIngestionDomainModelTest {

    private static final Instant NOW = Instant.parse("2026-05-27T00:00:00Z");

    @Test
    void createsRunSourceDocumentChunkAndErrorWithValidation() {
        PolicyIngestionSource source = sampleSource();
        PolicyIngestionRun run = sampleRun(source);
        PolicyIngestionDocument document = sampleDocument();
        PolicyIngestionChunk chunk = sampleChunk();
        PolicyIngestionError error = sampleError();

        assertThat(source.sourceType()).isEqualTo(PolicyIngestionSourceType.LOCAL_MARKDOWN);
        assertThat(run.status()).isEqualTo(PolicyIngestionStatus.CREATED);
        assertThat(document.rawText()).contains("Policy text");
        assertThat(chunk.status()).isEqualTo(PolicyIngestionChunkStatus.CHUNKED);
        assertThat(error.message()).contains("Failed to parse");

        assertThatThrownBy(() -> new PolicyIngestionSource(
                " ",
                PolicyIngestionSourceType.LOCAL_MARKDOWN,
                null,
                "Policy file",
                null,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourceId");
        assertThatThrownBy(() -> new PolicyIngestionDocument(
                "doc-1",
                "run-1",
                "Return Policy",
                "RETURN",
                "electronics",
                "v1",
                PolicyIngestionSourceType.LOCAL_MARKDOWN,
                null,
                "Policy text",
                null,
                LocalDate.parse("2026-05-02"),
                LocalDate.parse("2026-05-01"),
                null,
                NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectiveTo");
        assertThatThrownBy(() -> new PolicyIngestionChunk(
                "chunk-1",
                "run-1",
                "doc-1",
                0,
                "Policy chunk",
                0,
                null,
                null,
                PolicyIngestionChunkStatus.CHUNKED,
                null,
                NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tokenEstimate");
    }

    @Test
    void allowsOnlyDocumentedStatusTransitionsAndKeepsTerminalStatesClosed() {
        PolicyIngestionRun run = sampleRun(sampleSource());

        PolicyIngestionRun running = run.transitionTo(PolicyIngestionStatus.RUNNING, NOW.plusSeconds(1), null);
        PolicyIngestionRun chunked = running.transitionTo(PolicyIngestionStatus.CHUNKED, NOW.plusSeconds(2), null);
        PolicyIngestionRun embedding = chunked.transitionTo(PolicyIngestionStatus.EMBEDDING, NOW.plusSeconds(3), null);
        PolicyIngestionRun completed = embedding.transitionTo(
                PolicyIngestionStatus.COMPLETED,
                NOW.plusSeconds(4),
                null);

        assertThat(completed.status()).isEqualTo(PolicyIngestionStatus.COMPLETED);
        assertThat(completed.finishedAt()).isEqualTo(NOW.plusSeconds(4));
        assertThat(PolicyIngestionStateMachine.canTransition(
                PolicyIngestionStatus.EMBEDDING,
                PolicyIngestionStatus.PARTIALLY_FAILED))
                .isTrue();
        assertThat(PolicyIngestionStatus.PARTIALLY_FAILED.isTerminal()).isTrue();

        assertThatThrownBy(() -> run.transitionTo(PolicyIngestionStatus.COMPLETED, NOW.plusSeconds(1), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Illegal policy ingestion status transition");
        assertThatThrownBy(() -> completed.transitionTo(PolicyIngestionStatus.RUNNING, NOW.plusSeconds(5), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED -> RUNNING");
    }

    @Test
    void sanitizesRunChunkAndErrorDetails() {
        String unsafe = "password=real-secret apiKey=sk-testsecret prompt=full raw prompt "
                + "D:\\data\\private\\policy.md token=abc123 " + "x".repeat(600);

        PolicyIngestionRun failed = sampleRun(sampleSource())
                .transitionTo(PolicyIngestionStatus.CANCELLED, NOW.plusSeconds(1), unsafe);
        PolicyIngestionChunk chunk = new PolicyIngestionChunk(
                "chunk-unsafe",
                "run-1",
                "doc-1",
                0,
                "Policy chunk",
                12,
                null,
                null,
                PolicyIngestionChunkStatus.FAILED,
                unsafe,
                NOW);
        PolicyIngestionError error = new PolicyIngestionError(
                "error-unsafe",
                "run-1",
                "doc-1",
                "chunk-unsafe",
                "PARSE_ERROR",
                unsafe,
                unsafe,
                NOW);

        assertSanitized(failed.errorMessage());
        assertSanitized(chunk.errorMessage());
        assertSanitized(error.message());
        assertSanitized(error.sanitizedDetails());
    }

    @Test
    void terminalRunRequiresFinishedAtAndNonTerminalRunCannotHaveFinishedAt() {
        PolicyIngestionSource source = sampleSource();

        assertThatThrownBy(() -> new PolicyIngestionRun(
                "run-1",
                source,
                PolicyIngestionStatus.COMPLETED,
                1,
                2,
                2,
                0,
                null,
                NOW,
                null,
                NOW,
                NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finishedAt");

        assertThatThrownBy(() -> new PolicyIngestionRun(
                "run-1",
                source,
                PolicyIngestionStatus.RUNNING,
                1,
                2,
                0,
                0,
                null,
                NOW,
                NOW.plusSeconds(1),
                NOW,
                NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finishedAt");
    }

    private static void assertSanitized(String value) {
        assertThat(value).doesNotContain("real-secret");
        assertThat(value).doesNotContain("sk-testsecret");
        assertThat(value).doesNotContain("full raw prompt");
        assertThat(value).doesNotContain("D:\\data");
        assertThat(value).doesNotContain("abc123");
        assertThat(value.length()).isLessThanOrEqualTo(500);
    }

    static PolicyIngestionSource sampleSource() {
        return new PolicyIngestionSource(
                "source-1",
                PolicyIngestionSourceType.LOCAL_MARKDOWN,
                "policy://return.md",
                "Return policy markdown",
                "checksum-source",
                null);
    }

    static PolicyIngestionRun sampleRun(PolicyIngestionSource source) {
        return new PolicyIngestionRun(
                "run-1",
                source,
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

    static PolicyIngestionDocument sampleDocument() {
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

    static PolicyIngestionChunk sampleChunk() {
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

    static PolicyIngestionError sampleError() {
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
