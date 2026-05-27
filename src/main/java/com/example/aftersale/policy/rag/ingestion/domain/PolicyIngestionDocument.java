package com.example.aftersale.policy.rag.ingestion.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record PolicyIngestionDocument(
        String ingestionDocumentId,
        String runId,
        String title,
        String category,
        String productType,
        String version,
        PolicyIngestionSourceType sourceType,
        String sourceUri,
        String rawText,
        String checksum,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String metadataJson,
        Instant createdAt) {

    public PolicyIngestionDocument {
        ingestionDocumentId = PolicyIngestionText.requireText(ingestionDocumentId, "ingestionDocumentId");
        runId = PolicyIngestionText.requireText(runId, "runId");
        title = PolicyIngestionText.requireText(title, "title");
        category = PolicyIngestionText.requireText(category, "category");
        productType = PolicyIngestionText.requireText(productType, "productType");
        version = PolicyIngestionText.requireText(version, "version");
        sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        sourceUri = PolicyIngestionText.optionalText(sourceUri, "sourceUri");
        rawText = PolicyIngestionText.requireText(rawText, "rawText");
        checksum = PolicyIngestionText.optionalText(checksum, "checksum");
        if (effectiveTo != null && effectiveFrom != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("effectiveTo must not be before effectiveFrom");
        }
        metadataJson = PolicyIngestionText.metadata(metadataJson);
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
