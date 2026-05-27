package com.example.aftersale.policy.rag.ingestion.application;

import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionChunk;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionDocument;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionRepository;
import java.util.Objects;

public class PolicyIngestionDedupService {

    private final PolicyIngestionRepository repository;
    private final PolicyContentChecksumService checksumService;

    public PolicyIngestionDedupService(PolicyIngestionRepository repository) {
        this(repository, new PolicyContentChecksumService());
    }

    public PolicyIngestionDedupService(
            PolicyIngestionRepository repository,
            PolicyContentChecksumService checksumService) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.checksumService = Objects.requireNonNull(checksumService, "checksumService must not be null");
    }

    public PolicyDedupDecision checkDocument(PolicyIngestionDocument document) {
        PolicyIngestionDocument normalizedDocument = Objects.requireNonNull(document, "document must not be null");
        String checksum = normalizedDocument.checksum();
        if (checksum == null) {
            checksum = checksumService.checksumDocument(normalizedDocument).value();
        }
        return checkDocumentChecksum(checksum);
    }

    public PolicyDedupDecision checkDocumentChecksum(String checksum) {
        String normalizedChecksum = requireChecksum(checksum);
        return repository.findDocumentByChecksum(normalizedChecksum)
                .map(document -> new PolicyDedupDecision(
                        PolicyDedupDecisionType.DUPLICATE_DOCUMENT,
                        "document checksum already exists in ingestion repository",
                        document.ingestionDocumentId(),
                        null))
                .orElseGet(() -> new PolicyDedupDecision(
                        PolicyDedupDecisionType.NEW_CONTENT,
                        "document checksum is new",
                        null,
                        null));
    }

    public PolicyDedupDecision checkChunk(PolicyIngestionChunk chunk) {
        PolicyIngestionChunk normalizedChunk = Objects.requireNonNull(chunk, "chunk must not be null");
        String checksum = normalizedChunk.checksum();
        if (checksum == null) {
            checksum = checksumService.checksumChunk(normalizedChunk).value();
        }
        return checkChunkChecksum(normalizedChunk.ingestionDocumentId(), checksum);
    }

    public PolicyDedupDecision checkChunkChecksum(String ingestionDocumentId, String checksum) {
        String normalizedDocumentId = requireText(ingestionDocumentId, "ingestionDocumentId");
        String normalizedChecksum = requireChecksum(checksum);
        return repository.findChunksByDocumentIdAndChecksum(normalizedDocumentId, normalizedChecksum).stream()
                .findFirst()
                .map(chunk -> new PolicyDedupDecision(
                        PolicyDedupDecisionType.DUPLICATE_CHUNK,
                        "chunk checksum already exists for ingestion document",
                        chunk.ingestionDocumentId(),
                        chunk.ingestionChunkId()))
                .orElseGet(() -> new PolicyDedupDecision(
                        PolicyDedupDecisionType.NEW_CONTENT,
                        "chunk checksum is new for ingestion document",
                        null,
                        null));
    }

    private static String requireChecksum(String checksum) {
        return requireText(checksum, "checksum");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
