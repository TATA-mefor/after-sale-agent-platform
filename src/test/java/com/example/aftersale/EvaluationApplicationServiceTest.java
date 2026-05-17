package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.aftersale.agent.application.evaluation.EvaluationApplicationService;
import com.example.aftersale.agent.application.evaluation.EvaluationCase;
import com.example.aftersale.agent.application.evaluation.EvaluationFailure;
import com.example.aftersale.agent.application.evaluation.EvaluationReport;
import com.example.aftersale.agent.application.evaluation.EvaluationResult;
import com.example.aftersale.agent.application.planner.SubtaskType;
import com.example.aftersale.ticket.domain.IntentType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EvaluationApplicationServiceTest {

    private static final Path DATASET_PATH = Path.of("docs/evaluation/aftersale_cases.jsonl");

    @Autowired
    private EvaluationApplicationService evaluationApplicationService;

    @Test
    void canReadJsonlEvaluationCases() {
        List<EvaluationCase> cases = evaluationApplicationService.loadCases(DATASET_PATH);

        assertThat(cases).hasSize(15);
        assertThat(cases.get(0).caseId()).isEqualTo("AS-EVAL-001");
        assertThat(cases.get(0).expected().intent()).isEqualTo(IntentType.RETURN_AND_REFUND);
        assertThat(cases.get(0).expected().tools())
                .containsExactly("get_order_by_id", "search_aftersale_policy", "add_ticket_note");
    }

    @Test
    void canRunEvaluationAndGenerateReport() {
        EvaluationReport report = evaluationApplicationService.runRuleBased(DATASET_PATH);

        assertThat(report.totalCases()).isEqualTo(15);
        assertThat(report.passedCases()).isGreaterThanOrEqualTo(13);
        assertThat(report.failedCases()).isLessThanOrEqualTo(2);
        assertThat(report.metrics())
                .extracting(metric -> metric.name())
                .contains(
                        "intentAccuracy",
                        "subtaskTypeAccuracy",
                        "toolCallAccuracy",
                        "riskLevelAccuracy",
                        "policyMatchAccuracy",
                        "approvalRequirementAccuracy",
                        "planValidityRate");
        assertThat(report.planValidityRate()).isEqualTo(1.0D);
    }

    @Test
    void singleIntentCaseCanPass() {
        EvaluationReport report = evaluationApplicationService.runRuleBased(DATASET_PATH);

        EvaluationResult result = resultByCaseId(report, "AS-EVAL-001");
        assertThat(result.passed()).isTrue();
        assertThat(result.actualIntent()).isEqualTo(IntentType.RETURN_AND_REFUND);
        assertThat(result.actualPolicyCategories()).contains("质量问题退换货规则");
    }

    @Test
    void multiIntentCaseEvaluatesSubtaskTypes() {
        EvaluationReport report = evaluationApplicationService.runRuleBased(DATASET_PATH);

        EvaluationResult result = resultByCaseId(report, "AS-EVAL-012");
        assertThat(result.passed()).isTrue();
        assertThat(result.actualSubtaskTypes())
                .containsExactly(SubtaskType.RETURN, SubtaskType.EXCHANGE, SubtaskType.COUPON_CONSULTATION);
    }

    @Test
    void expectedToolsAreCheckedAgainstPlannedTools() {
        EvaluationReport report = evaluationApplicationService.runRuleBased(DATASET_PATH);

        EvaluationResult result = resultByCaseId(report, "AS-EVAL-002");
        assertThat(result.actualTools())
                .containsExactly("get_order_by_id", "search_aftersale_policy", "add_ticket_note");
        assertThat(result.failures())
                .noneMatch(failure -> failure.field().equals("plannedTools"));
    }

    @Test
    void approvalExpectationPassesForHighRiskFallback() {
        EvaluationReport report = evaluationApplicationService.runRuleBased(DATASET_PATH);

        EvaluationResult result = resultByCaseId(report, "AS-EVAL-013");
        assertThat(result.passed()).isTrue();
        assertThat(result.actualRiskLevel().requiresApproval()).isTrue();
        assertThat(result.failures())
                .extracting(EvaluationFailure::field)
                .doesNotContain("riskLevel", "approvalRequirement");
    }

    @Test
    void invalidCaseReturnsClearFailure(@TempDir Path tempDir) throws IOException {
        Path invalidDataset = tempDir.resolve("invalid.jsonl");
        Files.writeString(invalidDataset, "{\"caseId\":\"BROKEN\",\"input\":\"missing required fields\"}");

        assertThatThrownBy(() -> evaluationApplicationService.loadCases(invalidDataset))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid evaluation case at line 1")
                .hasMessageContaining("expectedIntent");
    }

    @Test
    void defaultEvaluationUsesOfflineRuleBasedPlanner() {
        EvaluationReport report = evaluationApplicationService.runRuleBased(DATASET_PATH);

        assertThat(report.totalCases()).isEqualTo(15);
        assertThat(report.planValidityRate()).isEqualTo(1.0D);
        assertThat(report.failures())
                .noneMatch(failure -> failure.message().contains("OPENAI_API_KEY"));
    }

    private static EvaluationResult resultByCaseId(EvaluationReport report, String caseId) {
        return report.results().stream()
                .filter(result -> result.caseId().equals(caseId))
                .findFirst()
                .orElseThrow();
    }
}
