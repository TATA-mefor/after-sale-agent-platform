package com.example.aftersale.policy.rag.evaluation;

import com.example.aftersale.policy.rag.search.RagPolicyEvidenceSource;
import com.example.aftersale.policy.rag.search.RetrievalMode;
import java.util.List;
import java.util.Objects;

public record RagEvaluationResult(
        String caseId,
        boolean passed,
        RetrievalMode retrievalMode,
        int evidenceCount,
        boolean fallbackUsed,
        boolean emptyResult,
        boolean citationComplete,
        boolean safetyPassed,
        List<RagPolicyEvidenceSource> evidenceSources,
        List<String> categories,
        List<String> documentIds,
        List<String> chunkIds,
        List<RagEvaluationFailure> failures) {

    public RagEvaluationResult {
        caseId = requireText(caseId, "caseId");
        retrievalMode = Objects.requireNonNull(retrievalMode, "retrievalMode must not be null");
        if (evidenceCount < 0) {
            throw new IllegalArgumentException("evidenceCount must not be negative");
        }
        evidenceSources = List.copyOf(Objects.requireNonNull(evidenceSources, "evidenceSources must not be null"));
        categories = List.copyOf(Objects.requireNonNull(categories, "categories must not be null"));
        documentIds = List.copyOf(Objects.requireNonNull(documentIds, "documentIds must not be null"));
        chunkIds = List.copyOf(Objects.requireNonNull(chunkIds, "chunkIds must not be null"));
        failures = List.copyOf(Objects.requireNonNull(failures, "failures must not be null"));
    }

    public boolean passedField(String fieldName) {
        return failures.stream().noneMatch(failure -> failure.field().equals(fieldName));
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
