package io.github.tatame.aftersale.policy.rag.ingestion.domain;

import java.util.Objects;

public record PolicyIngestionSource(
        String sourceId,
        PolicyIngestionSourceType sourceType,
        String sourceUri,
        String displayName,
        String checksum,
        String metadataJson) {

    public PolicyIngestionSource {
        sourceId = PolicyIngestionText.requireText(sourceId, "sourceId");
        sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        sourceUri = PolicyIngestionText.optionalText(sourceUri, "sourceUri");
        displayName = PolicyIngestionText.requireText(displayName, "displayName");
        checksum = PolicyIngestionText.optionalText(checksum, "checksum");
        metadataJson = PolicyIngestionText.metadata(metadataJson);
    }
}
