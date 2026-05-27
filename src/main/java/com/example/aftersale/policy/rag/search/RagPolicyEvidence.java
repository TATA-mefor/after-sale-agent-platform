package com.example.aftersale.policy.rag.search;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

public record RagPolicyEvidence(
        String evidenceId,
        String documentId,
        String chunkId,
        String policyId,
        String documentTitle,
        String category,
        String productType,
        String snippet,
        double score,
        Double keywordScore,
        Double vectorScore,
        RetrievalMode retrievalMode,
        RagPolicyEvidenceSource source,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String metadataJson) {

    private static final int MAX_METADATA_LENGTH = 1_000;

    public RagPolicyEvidence {
        evidenceId = requireText(evidenceId, "evidenceId");
        documentId = optionalText(documentId, "documentId");
        chunkId = optionalText(chunkId, "chunkId");
        policyId = optionalText(policyId, "policyId");
        documentTitle = optionalText(documentTitle, "documentTitle");
        category = requireText(category, "category");
        productType = requireText(productType, "productType");
        snippet = requireText(snippet, "snippet");
        validateEvidenceText(snippet);
        score = requireScore(score, "score");
        keywordScore = optionalScore(keywordScore, "keywordScore");
        vectorScore = optionalScore(vectorScore, "vectorScore");
        retrievalMode = Objects.requireNonNull(retrievalMode, "retrievalMode must not be null");
        source = Objects.requireNonNull(source, "source must not be null");
        if (effectiveFrom != null && effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("effectiveTo must not be before effectiveFrom");
        }
        metadataJson = normalizeMetadata(metadataJson);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String optionalText(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank when provided");
        }
        return normalized;
    }

    private static double requireScore(double value, String fieldName) {
        if (value < 0.0d || value > 1.0d || !Double.isFinite(value)) {
            throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
        }
        return value;
    }

    private static Double optionalScore(Double value, String fieldName) {
        if (value != null) {
            requireScore(value, fieldName);
        }
        return value;
    }

    private static void validateEvidenceText(String value) {
        if (containsCompletedBusinessAction(value)) {
            throw new IllegalArgumentException("snippet must not claim completed business actions");
        }
    }

    private static String normalizeMetadata(String value) {
        if (value == null || value.isBlank()) {
            return "{}";
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_METADATA_LENGTH) {
            throw new IllegalArgumentException("metadataJson must not contain long raw text");
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("api_key")
                || lower.contains("apikey")
                || lower.contains("password")
                || lower.contains("token")
                || lower.contains("prompt")
                || lower.contains("d:\\")
                || lower.contains("c:\\")
                || lower.contains("/users/")) {
            throw new IllegalArgumentException("metadataJson must not contain secrets, prompts, or local paths");
        }
        return normalized;
    }

    private static boolean containsCompletedBusinessAction(String value) {
        return value.contains("已退款")
                || value.contains("已换货")
                || value.contains("已补偿")
                || value.contains("已关闭争议")
                || value.toLowerCase(Locale.ROOT).contains("refund completed")
                || value.toLowerCase(Locale.ROOT).contains("exchange completed")
                || value.toLowerCase(Locale.ROOT).contains("compensation completed");
    }
}
