package io.github.tatame.aftersale.policy.rag.evaluation;

import java.util.List;
import java.util.Objects;

public record RagEvaluationReport(
        int totalCases,
        int passedCases,
        int failedCases,
        double passRate,
        double evidenceRecallPassRate,
        double evidenceSourcePassRate,
        double retrievalModePassRate,
        double fallbackAccuracy,
        double emptyResultAccuracy,
        double citationCompletenessRate,
        double safetyPassRate,
        double averageEvidenceCount,
        List<RagEvaluationMetric> metrics,
        List<RagEvaluationResult> results,
        List<RagEvaluationFailure> failures) {

    public RagEvaluationReport {
        if (totalCases < 0 || passedCases < 0 || failedCases < 0 || passedCases + failedCases != totalCases) {
            throw new IllegalArgumentException("case counts must be valid");
        }
        passRate = requireRate(passRate, "passRate");
        evidenceRecallPassRate = requireRate(evidenceRecallPassRate, "evidenceRecallPassRate");
        evidenceSourcePassRate = requireRate(evidenceSourcePassRate, "evidenceSourcePassRate");
        retrievalModePassRate = requireRate(retrievalModePassRate, "retrievalModePassRate");
        fallbackAccuracy = requireRate(fallbackAccuracy, "fallbackAccuracy");
        emptyResultAccuracy = requireRate(emptyResultAccuracy, "emptyResultAccuracy");
        citationCompletenessRate = requireRate(citationCompletenessRate, "citationCompletenessRate");
        safetyPassRate = requireRate(safetyPassRate, "safetyPassRate");
        if (averageEvidenceCount < 0.0d || !Double.isFinite(averageEvidenceCount)) {
            throw new IllegalArgumentException("averageEvidenceCount must be a non-negative finite number");
        }
        metrics = List.copyOf(Objects.requireNonNull(metrics, "metrics must not be null"));
        results = List.copyOf(Objects.requireNonNull(results, "results must not be null"));
        failures = List.copyOf(Objects.requireNonNull(failures, "failures must not be null"));
    }

    private static double requireRate(double value, String fieldName) {
        if (value < 0.0d || value > 1.0d || !Double.isFinite(value)) {
            throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
        }
        return value;
    }
}
