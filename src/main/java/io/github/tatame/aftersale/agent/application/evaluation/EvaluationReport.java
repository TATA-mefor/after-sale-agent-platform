package io.github.tatame.aftersale.agent.application.evaluation;

import java.util.List;
import java.util.Objects;

public record EvaluationReport(
        int totalCases,
        int passedCases,
        int failedCases,
        double intentAccuracy,
        double subtaskTypeAccuracy,
        double toolCallAccuracy,
        double riskLevelAccuracy,
        double policyMatchAccuracy,
        double approvalRequirementAccuracy,
        double planValidityRate,
        List<EvaluationMetric> metrics,
        List<EvaluationResult> results,
        List<EvaluationFailure> failures) {

    public EvaluationReport {
        if (totalCases < 0 || passedCases < 0 || failedCases < 0) {
            throw new IllegalArgumentException("case counts must not be negative");
        }
        metrics = List.copyOf(Objects.requireNonNull(metrics, "metrics must not be null"));
        results = List.copyOf(Objects.requireNonNull(results, "results must not be null"));
        failures = List.copyOf(Objects.requireNonNull(failures, "failures must not be null"));
    }
}
