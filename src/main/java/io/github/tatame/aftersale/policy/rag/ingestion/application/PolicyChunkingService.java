package io.github.tatame.aftersale.policy.rag.ingestion.application;

import io.github.tatame.aftersale.policy.rag.ingestion.domain.PolicyIngestionChunk;
import io.github.tatame.aftersale.policy.rag.ingestion.domain.PolicyIngestionChunkStatus;
import io.github.tatame.aftersale.policy.rag.ingestion.domain.PolicyIngestionDocument;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PolicyChunkingService {

    private final PolicyContentChecksumService checksumService;

    public PolicyChunkingService() {
        this(new PolicyContentChecksumService());
    }

    public PolicyChunkingService(PolicyContentChecksumService checksumService) {
        this.checksumService = Objects.requireNonNull(checksumService, "checksumService must not be null");
    }

    public PolicyChunkingResult chunk(PolicyIngestionDocument document) {
        return chunk(document, PolicyChunkingOptions.defaults(), Instant.now());
    }

    public PolicyChunkingResult chunk(
            PolicyIngestionDocument document,
            PolicyChunkingOptions options,
            Instant createdAt) {
        PolicyIngestionDocument normalizedDocument = Objects.requireNonNull(document, "document must not be null");
        PolicyChunkingOptions normalizedOptions = Objects.requireNonNull(options, "options must not be null");
        Instant normalizedCreatedAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        String text = requireRawText(normalizedDocument.rawText());
        List<PolicyIngestionChunk> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            if (chunks.size() >= normalizedOptions.maxChunksPerDocument()) {
                throw new IllegalArgumentException(
                        "document exceeds maxChunksPerDocument; split the source document or adjust chunking options");
            }
            int end = chooseChunkEnd(text, start, normalizedOptions);
            String content = text.substring(start, end).trim();
            if (content.isBlank()) {
                start = advanceStart(start, end, normalizedOptions.overlapChars(), text.length());
                continue;
            }
            int chunkIndex = chunks.size();
            PolicyChecksum checksum = checksumService.checksumContent(content);
            chunks.add(new PolicyIngestionChunk(
                    normalizedDocument.ingestionDocumentId() + "-chunk-" + chunkIndex,
                    normalizedDocument.runId(),
                    normalizedDocument.ingestionDocumentId(),
                    chunkIndex,
                    content,
                    estimateTokens(content, normalizedOptions.tokenEstimateDivisor()),
                    checksum.value(),
                    "{}",
                    PolicyIngestionChunkStatus.CHUNKED,
                    null,
                    normalizedCreatedAt));
            start = advanceStart(start, end, normalizedOptions.overlapChars(), text.length());
        }
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("rawText produced no non-empty chunks");
        }
        return new PolicyChunkingResult(normalizedDocument, chunks, normalizedOptions);
    }

    public int estimateTokens(String content, int tokenEstimateDivisor) {
        if (tokenEstimateDivisor <= 0) {
            throw new IllegalArgumentException("tokenEstimateDivisor must be positive");
        }
        String normalized = requireRawText(content);
        return Math.max(1, (int) Math.ceil((double) normalized.length() / tokenEstimateDivisor));
    }

    private static int chooseChunkEnd(String text, int start, PolicyChunkingOptions options) {
        int hardEnd = Math.min(start + options.maxChunkChars(), text.length());
        if (!options.preserveParagraphBoundary() || hardEnd == text.length()) {
            return hardEnd;
        }
        int lowerBound = Math.min(start + options.minChunkChars(), hardEnd);
        int paragraphEnd = text.lastIndexOf("\n\n", hardEnd);
        if (paragraphEnd >= lowerBound) {
            return paragraphEnd + 2;
        }
        int lineEnd = text.lastIndexOf('\n', hardEnd);
        if (lineEnd >= lowerBound) {
            return lineEnd + 1;
        }
        return hardEnd;
    }

    private static int advanceStart(int previousStart, int end, int overlapChars, int textLength) {
        if (end >= textLength) {
            return textLength;
        }
        int nextStart = Math.max(0, end - overlapChars);
        if (nextStart <= previousStart) {
            return end;
        }
        return nextStart;
    }

    private static String requireRawText(String rawText) {
        Objects.requireNonNull(rawText, "rawText must not be null");
        String normalized = rawText.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("rawText must not be blank");
        }
        return normalized;
    }
}
