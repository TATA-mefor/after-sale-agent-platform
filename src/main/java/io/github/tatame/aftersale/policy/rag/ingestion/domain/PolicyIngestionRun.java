package io.github.tatame.aftersale.policy.rag.ingestion.domain;

import java.time.Instant;
import java.util.Objects;

public record PolicyIngestionRun(
        String runId,
        PolicyIngestionSource source,
        PolicyIngestionStatus status,
        int totalDocuments,
        int totalChunks,
        int embeddedChunks,
        int failedChunks,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt,
        Instant updatedAt) {

    public PolicyIngestionRun {
        runId = PolicyIngestionText.requireText(runId, "runId");
        source = Objects.requireNonNull(source, "source must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        requireNonNegative(totalDocuments, "totalDocuments");
        requireNonNegative(totalChunks, "totalChunks");
        requireNonNegative(embeddedChunks, "embeddedChunks");
        requireNonNegative(failedChunks, "failedChunks");
        errorMessage = PolicyIngestionText.sanitizedOptionalText(errorMessage, "errorMessage");
        startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (finishedAt != null && finishedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("finishedAt must not be before startedAt");
        }
        if (status.isTerminal() && finishedAt == null) {
            throw new IllegalArgumentException("finishedAt must be provided for terminal ingestion status");
        }
        if (!status.isTerminal() && finishedAt != null) {
            throw new IllegalArgumentException("finishedAt must be null for non-terminal ingestion status");
        }
    }

    public PolicyIngestionRun transitionTo(
            PolicyIngestionStatus nextStatus,
            Instant changedAt,
            String nextErrorMessage) {
        Objects.requireNonNull(nextStatus, "nextStatus must not be null");
        Instant transitionTime = Objects.requireNonNull(changedAt, "changedAt must not be null");
        PolicyIngestionStateMachine.requireValidTransition(status, nextStatus);
        Instant nextFinishedAt = nextStatus.isTerminal() ? transitionTime : null;
        return new PolicyIngestionRun(
                runId,
                source,
                nextStatus,
                totalDocuments,
                totalChunks,
                embeddedChunks,
                failedChunks,
                nextErrorMessage,
                startedAt,
                nextFinishedAt,
                createdAt,
                transitionTime);
    }

    public PolicyIngestionRun withStatistics(
            int nextTotalDocuments,
            int nextTotalChunks,
            int nextEmbeddedChunks,
            int nextFailedChunks,
            Instant changedAt) {
        return new PolicyIngestionRun(
                runId,
                source,
                status,
                nextTotalDocuments,
                nextTotalChunks,
                nextEmbeddedChunks,
                nextFailedChunks,
                errorMessage,
                startedAt,
                finishedAt,
                createdAt,
                Objects.requireNonNull(changedAt, "changedAt must not be null"));
    }

    private static void requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than or equal to zero");
        }
    }
}
