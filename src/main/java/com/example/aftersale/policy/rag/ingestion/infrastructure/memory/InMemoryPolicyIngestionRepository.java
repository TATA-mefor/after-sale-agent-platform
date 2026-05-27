package com.example.aftersale.policy.rag.ingestion.infrastructure.memory;

import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionChunk;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionDocument;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionError;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionRepository;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionRun;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Offline ingestion repository for deterministic pipeline foundation tests.
 */
public class InMemoryPolicyIngestionRepository implements PolicyIngestionRepository {

    private final Map<String, PolicyIngestionRun> runs = new LinkedHashMap<>();
    private final Map<String, PolicyIngestionDocument> documents = new LinkedHashMap<>();
    private final Map<String, PolicyIngestionChunk> chunks = new LinkedHashMap<>();
    private final Map<String, PolicyIngestionError> errors = new LinkedHashMap<>();

    @Override
    public PolicyIngestionRun saveRun(PolicyIngestionRun run) {
        PolicyIngestionRun normalized = Objects.requireNonNull(run, "run must not be null");
        rejectDuplicate(runs.containsKey(normalized.runId()), "run", normalized.runId());
        runs.put(normalized.runId(), normalized);
        return normalized;
    }

    @Override
    public Optional<PolicyIngestionRun> findRunById(String runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    @Override
    public PolicyIngestionRun updateRun(PolicyIngestionRun run) {
        PolicyIngestionRun normalized = Objects.requireNonNull(run, "run must not be null");
        requireRun(normalized.runId());
        runs.put(normalized.runId(), normalized);
        return normalized;
    }

    @Override
    public PolicyIngestionDocument saveDocument(PolicyIngestionDocument document) {
        PolicyIngestionDocument normalized = Objects.requireNonNull(document, "document must not be null");
        requireRun(normalized.runId());
        rejectDuplicate(
                documents.containsKey(normalized.ingestionDocumentId()),
                "document",
                normalized.ingestionDocumentId());
        documents.put(normalized.ingestionDocumentId(), normalized);
        return normalized;
    }

    @Override
    public List<PolicyIngestionDocument> findDocumentsByRunId(String runId) {
        return documents.values().stream()
                .filter(document -> document.runId().equals(runId))
                .sorted(Comparator.comparing(PolicyIngestionDocument::createdAt)
                        .thenComparing(PolicyIngestionDocument::ingestionDocumentId))
                .toList();
    }

    @Override
    public Optional<PolicyIngestionDocument> findDocumentByChecksum(String checksum) {
        String normalizedChecksum = requireText(checksum, "checksum");
        return documents.values().stream()
                .filter(document -> normalizedChecksum.equals(document.checksum()))
                .findFirst();
    }

    @Override
    public PolicyIngestionChunk saveChunk(PolicyIngestionChunk chunk) {
        PolicyIngestionChunk normalized = Objects.requireNonNull(chunk, "chunk must not be null");
        requireRun(normalized.runId());
        requireDocument(normalized.ingestionDocumentId());
        rejectDuplicate(chunks.containsKey(normalized.ingestionChunkId()), "chunk", normalized.ingestionChunkId());
        chunks.put(normalized.ingestionChunkId(), normalized);
        return normalized;
    }

    @Override
    public List<PolicyIngestionChunk> findChunksByRunId(String runId) {
        return chunks.values().stream()
                .filter(chunk -> chunk.runId().equals(runId))
                .sorted(Comparator.comparingInt(PolicyIngestionChunk::chunkIndex)
                        .thenComparing(PolicyIngestionChunk::ingestionChunkId))
                .toList();
    }

    @Override
    public List<PolicyIngestionChunk> findChunksByDocumentId(String ingestionDocumentId) {
        return chunks.values().stream()
                .filter(chunk -> chunk.ingestionDocumentId().equals(ingestionDocumentId))
                .sorted(Comparator.comparingInt(PolicyIngestionChunk::chunkIndex)
                        .thenComparing(PolicyIngestionChunk::ingestionChunkId))
                .toList();
    }

    @Override
    public List<PolicyIngestionChunk> findChunksByChecksum(String checksum) {
        String normalizedChecksum = requireText(checksum, "checksum");
        return chunks.values().stream()
                .filter(chunk -> normalizedChecksum.equals(chunk.checksum()))
                .sorted(Comparator.comparing(PolicyIngestionChunk::ingestionDocumentId)
                        .thenComparingInt(PolicyIngestionChunk::chunkIndex)
                        .thenComparing(PolicyIngestionChunk::ingestionChunkId))
                .toList();
    }

    @Override
    public List<PolicyIngestionChunk> findChunksByDocumentIdAndChecksum(
            String ingestionDocumentId,
            String checksum) {
        String normalizedDocumentId = requireText(ingestionDocumentId, "ingestionDocumentId");
        String normalizedChecksum = requireText(checksum, "checksum");
        return chunks.values().stream()
                .filter(chunk -> chunk.ingestionDocumentId().equals(normalizedDocumentId))
                .filter(chunk -> normalizedChecksum.equals(chunk.checksum()))
                .sorted(Comparator.comparingInt(PolicyIngestionChunk::chunkIndex)
                        .thenComparing(PolicyIngestionChunk::ingestionChunkId))
                .toList();
    }

    @Override
    public PolicyIngestionError saveError(PolicyIngestionError error) {
        PolicyIngestionError normalized = Objects.requireNonNull(error, "error must not be null");
        requireRun(normalized.runId());
        rejectDuplicate(errors.containsKey(normalized.errorId()), "error", normalized.errorId());
        errors.put(normalized.errorId(), normalized);
        return normalized;
    }

    @Override
    public List<PolicyIngestionError> findErrorsByRunId(String runId) {
        return errors.values().stream()
                .filter(error -> error.runId().equals(runId))
                .sorted(Comparator.comparing(PolicyIngestionError::createdAt)
                        .thenComparing(PolicyIngestionError::errorId))
                .toList();
    }

    private void requireRun(String runId) {
        if (!runs.containsKey(runId)) {
            throw new IllegalArgumentException("run must be saved before child ingestion records: " + runId);
        }
    }

    private void requireDocument(String ingestionDocumentId) {
        if (!documents.containsKey(ingestionDocumentId)) {
            throw new IllegalArgumentException(
                    "document must be saved before ingestion chunk: " + ingestionDocumentId);
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static void rejectDuplicate(boolean duplicate, String entityType, String entityId) {
        if (duplicate) {
            throw new IllegalArgumentException("duplicate ingestion " + entityType + " is not allowed: " + entityId);
        }
    }
}
