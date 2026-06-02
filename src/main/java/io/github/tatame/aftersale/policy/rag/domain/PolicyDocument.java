package io.github.tatame.aftersale.policy.rag.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record PolicyDocument(
        String documentId,
        String title,
        String category,
        String productType,
        String version,
        PolicyDocumentSourceType sourceType,
        String sourceUri,
        String checksum,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Instant createdAt,
        Instant updatedAt) {

    public PolicyDocument {
        documentId = requireText(documentId, "documentId");
        title = requireText(title, "title");
        category = requireText(category, "category");
        productType = requireText(productType, "productType");
        version = requireText(version, "version");
        sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        sourceUri = optionalText(sourceUri);
        checksum = requireText(checksum, "checksum");
        effectiveFrom = Objects.requireNonNull(effectiveFrom, "effectiveFrom must not be null");
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("effectiveTo must not be before effectiveFrom");
        }
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("sourceUri must not be blank when provided");
        }
        return normalized;
    }
}
