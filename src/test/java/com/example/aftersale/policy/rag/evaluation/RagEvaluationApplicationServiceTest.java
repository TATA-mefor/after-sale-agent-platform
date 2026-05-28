package com.example.aftersale.policy.rag.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RagEvaluationApplicationServiceTest {

    private static final Path DATASET_PATH = Path.of("docs/evaluation/rag_policy_cases.jsonl");

    private final RagEvaluationApplicationService service = new RagEvaluationApplicationService(new ObjectMapper());

    @Test
    void runnerLoadsAllCasesAndProducesPassingReport() {
        RagEvaluationReport report = service.run(DATASET_PATH);

        assertThat(report.totalCases()).isEqualTo(15);
        assertThat(report.passedCases()).isEqualTo(15);
        assertThat(report.failedCases()).isZero();
        assertThat(report.passRate()).isEqualTo(1.0d);
        assertThat(report.failures()).isEmpty();
        assertThat(report.metrics())
                .extracting(RagEvaluationMetric::name)
                .containsExactly(
                        "passRate",
                        "evidenceRecallPassRate",
                        "evidenceSourcePassRate",
                        "retrievalModePassRate",
                        "fallbackAccuracy",
                        "emptyResultAccuracy",
                        "citationCompletenessRate",
                        "safetyPassRate");
    }

    @Test
    void metricsAreCalculatedDeterministically() {
        RagEvaluationReport report = service.run(DATASET_PATH);

        assertThat(report.evidenceRecallPassRate()).isEqualTo(1.0d);
        assertThat(report.evidenceSourcePassRate()).isEqualTo(1.0d);
        assertThat(report.retrievalModePassRate()).isEqualTo(1.0d);
        assertThat(report.fallbackAccuracy()).isEqualTo(1.0d);
        assertThat(report.emptyResultAccuracy()).isEqualTo(1.0d);
        assertThat(report.citationCompletenessRate()).isEqualTo(1.0d);
        assertThat(report.safetyPassRate()).isEqualTo(1.0d);
        assertThat(report.averageEvidenceCount()).isGreaterThan(0.0d);
    }

    @Test
    void unsupportedAndEmptyCasesDoNotFabricateEvidence() {
        RagEvaluationReport report = service.run(DATASET_PATH);

        assertThat(result(report, "rag-unsupported-empty-009").emptyResult()).isTrue();
        assertThat(result(report, "rag-unsupported-empty-009").evidenceCount()).isZero();
        assertThat(result(report, "rag-empty-vector-filter-010").emptyResult()).isTrue();
        assertThat(result(report, "rag-empty-vector-filter-010").evidenceCount()).isZero();
    }

    @Test
    void fallbackCitationAndSafetyExpectationsAreChecked() {
        RagEvaluationReport report = service.run(DATASET_PATH);

        assertThat(result(report, "rag-vector-only-011").fallbackUsed()).isTrue();
        assertThat(result(report, "rag-keyword-fallback-012").fallbackUsed()).isTrue();
        assertThat(report.results()).allSatisfy(result -> {
            assertThat(result.safetyPassed()).isTrue();
            assertThat(result.failures()).allSatisfy(failure -> {
                assertThat(failure.caseId()).isNotBlank();
                assertThat(failure.field()).isNotBlank();
                assertThat(failure.expected()).isNotBlank();
                assertThat(failure.actual()).isNotBlank();
                assertThat(failure.message()).isNotBlank();
            });
        });
    }

    @Test
    void defaultRunnerUsesOfflineFakeInMemoryDependencies() {
        RagEvaluationReport report = service.runDefault();

        assertThat(report.totalCases()).isEqualTo(15);
        assertThat(report.failures())
                .noneMatch(failure -> failure.message().contains("OPENAI_API_KEY"))
                .noneMatch(failure -> failure.message().contains("PostgreSQL"))
                .noneMatch(failure -> failure.message().contains("PGvector"));
    }

    private static RagEvaluationResult result(RagEvaluationReport report, String caseId) {
        return report.results().stream()
                .filter(result -> result.caseId().equals(caseId))
                .findFirst()
                .orElseThrow();
    }
}
