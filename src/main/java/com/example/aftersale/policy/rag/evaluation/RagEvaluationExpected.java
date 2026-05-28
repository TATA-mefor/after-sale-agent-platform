package com.example.aftersale.policy.rag.evaluation;

import com.example.aftersale.policy.rag.search.RagPolicyEvidenceSource;
import com.example.aftersale.policy.rag.search.RetrievalMode;
import java.util.List;
import java.util.Objects;

public record RagEvaluationExpected(
        RetrievalMode requiredRetrievalMode,
        List<RagPolicyEvidenceSource> requiredEvidenceSources,
        List<String> requiredCategories,
        List<String> requiredProductTypes,
        List<String> requiredAnySnippetContains,
        List<String> forbiddenSnippetContains,
        int minEvidenceCount,
        int maxEvidenceCount,
        boolean expectFallbackUsed,
        boolean expectEmptyResult,
        boolean requireCitation,
        List<String> requiredDocumentIds,
        List<String> requiredChunkIds) {

    public RagEvaluationExpected {
        requiredRetrievalMode = Objects.requireNonNull(
                requiredRetrievalMode, "requiredRetrievalMode must not be null");
        requiredEvidenceSources = List.copyOf(Objects.requireNonNull(
                requiredEvidenceSources, "requiredEvidenceSources must not be null"));
        requiredCategories = normalizeList(requiredCategories, "requiredCategories");
        requiredProductTypes = normalizeList(requiredProductTypes, "requiredProductTypes");
        requiredAnySnippetContains = normalizeList(requiredAnySnippetContains, "requiredAnySnippetContains");
        forbiddenSnippetContains = normalizeList(forbiddenSnippetContains, "forbiddenSnippetContains");
        if (minEvidenceCount < 0 || maxEvidenceCount < minEvidenceCount) {
            throw new IllegalArgumentException("evidence count bounds must be valid");
        }
        requiredDocumentIds = normalizeList(requiredDocumentIds, "requiredDocumentIds");
        requiredChunkIds = normalizeList(requiredChunkIds, "requiredChunkIds");
    }

    public List<RagPolicyEvidenceSource> requiredEvidenceSources() {
        return List.copyOf(requiredEvidenceSources);
    }

    public List<String> requiredCategories() {
        return List.copyOf(requiredCategories);
    }

    public List<String> requiredProductTypes() {
        return List.copyOf(requiredProductTypes);
    }

    public List<String> requiredAnySnippetContains() {
        return List.copyOf(requiredAnySnippetContains);
    }

    public List<String> forbiddenSnippetContains() {
        return List.copyOf(forbiddenSnippetContains);
    }

    public List<String> requiredDocumentIds() {
        return List.copyOf(requiredDocumentIds);
    }

    public List<String> requiredChunkIds() {
        return List.copyOf(requiredChunkIds);
    }

    private static List<String> normalizeList(List<String> values, String fieldName) {
        return List.copyOf(Objects.requireNonNull(values, fieldName + " must not be null")).stream()
                .map(value -> requireText(value, fieldName))
                .toList();
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not contain null values");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not contain blank values");
        }
        return normalized;
    }
}
