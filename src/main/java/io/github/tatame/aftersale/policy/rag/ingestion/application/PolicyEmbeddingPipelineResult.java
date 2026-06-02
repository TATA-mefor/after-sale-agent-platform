package io.github.tatame.aftersale.policy.rag.ingestion.application;

import io.github.tatame.aftersale.policy.rag.ingestion.domain.PolicyIngestionStatus;
import java.util.List;
import java.util.Objects;

public record PolicyEmbeddingPipelineResult(
        String runId,
        int processedChunks,
        int embeddedChunks,
        int skippedChunks,
        int failedChunks,
        int savedDocuments,
        int savedVectorChunks,
        int savedEmbeddings,
        List<PolicyEmbeddingPipelineFailure> failures,
        PolicyIngestionStatus status) {

    public PolicyEmbeddingPipelineResult {
        runId = requireText(runId, "runId");
        requireNonNegative(processedChunks, "processedChunks");
        requireNonNegative(embeddedChunks, "embeddedChunks");
        requireNonNegative(skippedChunks, "skippedChunks");
        requireNonNegative(failedChunks, "failedChunks");
        requireNonNegative(savedDocuments, "savedDocuments");
        requireNonNegative(savedVectorChunks, "savedVectorChunks");
        requireNonNegative(savedEmbeddings, "savedEmbeddings");
        failures = List.copyOf(Objects.requireNonNull(failures, "failures must not be null"));
        status = Objects.requireNonNull(status, "status must not be null");
    }

    private static void requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than or equal to zero");
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
}
