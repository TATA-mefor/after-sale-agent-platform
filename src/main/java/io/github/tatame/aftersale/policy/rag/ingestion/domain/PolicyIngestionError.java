package io.github.tatame.aftersale.policy.rag.ingestion.domain;

import java.time.Instant;
import java.util.Objects;

public record PolicyIngestionError(
        String errorId,
        String runId,
        String ingestionDocumentId,
        String ingestionChunkId,
        String errorCode,
        String message,
        String sanitizedDetails,
        Instant createdAt) {

    public PolicyIngestionError {
        errorId = PolicyIngestionText.requireText(errorId, "errorId");
        runId = PolicyIngestionText.requireText(runId, "runId");
        ingestionDocumentId = PolicyIngestionText.optionalText(ingestionDocumentId, "ingestionDocumentId");
        ingestionChunkId = PolicyIngestionText.optionalText(ingestionChunkId, "ingestionChunkId");
        errorCode = PolicyIngestionText.requireText(errorCode, "errorCode");
        message = PolicyIngestionText.sanitizedOptionalText(
                PolicyIngestionText.requireText(message, "message"), "message");
        sanitizedDetails = PolicyIngestionText.sanitizedOptionalText(sanitizedDetails, "sanitizedDetails");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
