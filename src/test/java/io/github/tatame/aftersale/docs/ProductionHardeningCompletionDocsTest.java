package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ProductionHardeningCompletionDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String V5B_COMPLETION =
            "docs/exec-plans/completed/EXEC_PLAN_V5_B_PRODUCTION_HARDENING_COMPLETION.md";
    private static final String V5B_SUMMARY =
            "docs/deploy/PRODUCTION_HARDENING_COMPLETION_SUMMARY.md";

    private static final List<String> ALL_B_COMPLETION_RECORDS = List.of(
            "docs/exec-plans/completed/EXEC_PLAN_V5_B1_CONTAINER_CI.md",
            "docs/exec-plans/completed/EXEC_PLAN_V5_B2_1_CONFIG_SECRET_BOUNDARY.md",
            "docs/exec-plans/completed/EXEC_PLAN_V5_B2_2_FLYWAY_MIGRATION_FOUNDATION.md",
            "docs/exec-plans/completed/EXEC_PLAN_V5_B2_3_PROFILE_MATRIX_VALIDATION.md",
            "docs/exec-plans/completed/EXEC_PLAN_V5_B3_5_OBSERVABILITY_DOCS_COMPLETION_RECORD.md",
            "docs/exec-plans/completed/EXEC_PLAN_V5_B4_1_AUTH_RBAC_BOUNDARY_DECISION.md",
            "docs/exec-plans/completed/EXEC_PLAN_V5_B4_2_SPRING_SECURITY_API_KEY_AUTH_FOUNDATION.md",
            "docs/exec-plans/completed/EXEC_PLAN_V5_B4_3_K8S_HELM_FOUNDATION.md",
            "docs/exec-plans/completed/EXEC_PLAN_V5_B4_4_RELEASE_ROLLBACK_FOUNDATION.md");

    private static final List<String> ALL_V5B_DOCS = List.of(
            "README.md",
            V5B_COMPLETION,
            V5B_SUMMARY,
            "docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "docs/deploy/CONTAINER_CI_HARDENING.md",
            "docs/deploy/OBSERVABILITY_DOCS_COMPLETION.md",
            "docs/deploy/AUTH_RUNTIME_FOUNDATION.md",
            "docs/deploy/AUTH_RBAC_BOUNDARY.md",
            "docs/deploy/K8S_HELM_FOUNDATION.md",
            "docs/deploy/RELEASE_ROLLBACK_FOUNDATION.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md");

    // ---- A. File existence ----

    @Test
    void v5bCompletionRecordAndSummaryExist() throws IOException {
        assertThat(PROJECT_ROOT.resolve(V5B_COMPLETION)).exists();
        assertThat(PROJECT_ROOT.resolve(V5B_SUMMARY)).exists();
    }

    @Test
    void allBPhaseCompletionRecordsExist() throws IOException {
        for (String path : ALL_B_COMPLETION_RECORDS) {
            assertThat(PROJECT_ROOT.resolve(path)).as(path + " should exist").exists();
        }
    }

    @Test
    void validationAndQualityDocsExist() throws IOException {
        assertThat(PROJECT_ROOT.resolve("docs/quality/VALIDATION_COMMANDS.md")).exists();
        assertThat(PROJECT_ROOT.resolve("docs/quality/QUALITY_SCORE.md")).exists();
    }

    @Test
    void v5bCompletionRecordContainsTaskComplete() throws IOException {
        String record = projectText(V5B_COMPLETION);
        assertThat(record).contains("Status: Completed", "TASK_COMPLETE");
    }

    // ---- B. Docs status ----

    @Test
    void readmeMentionsV5BProductionHardeningCompletion() throws IOException {
        String readme = projectText("README.md");
        String lower = readme.toLowerCase(Locale.ROOT);
        assertThat(lower).contains("v5.b production hardening");
        assertThat(lower).contains("current planned");
        assertThat(lower).contains("scope completed");
        assertThat(lower).contains("production_hardening_completion_summary.md");
    }

    @Test
    void roadmapMarksV5BCurrentPlannedScopeCompleted() throws IOException {
        String roadmap = projectText("docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md");
        String lower = roadmap.toLowerCase(Locale.ROOT);
        assertThat(lower).contains(
                "v5.b production hardening current planned scope completed",
                "production deployment",
                "future");
    }

    @Test
    void qualityScoreMentionsV5BCurrentPlannedScopeCompleted() throws IOException {
        String quality = projectText("docs/quality/QUALITY_SCORE.md");
        assertThat(quality).contains(
                "V5.B Production Hardening",
                "current planned scope completed");
    }

    @Test
    void validationDocsMentionProductionHardeningCompletionTest() throws IOException {
        String validation = projectText("docs/quality/VALIDATION_COMMANDS.md");
        assertThat(validation).contains(
                "mvn test -Dtest=ProductionHardeningCompletionDocsTest",
                "V5.B Production Hardening Completion Validation");
    }

    // ---- C. Stage summaries ----

    @Test
    void v5bCompletionDocSummarizesAllFourPhases() throws IOException {
        String doc = projectText(V5B_COMPLETION);
        assertThat(doc).contains(
                "V5.B.1 Container + CI",
                "V5.B.2 Config + Secret + Migration",
                "V5.B.3 Observability",
                "V5.B.4 Auth + K8s + Release/Rollback");
    }

    @Test
    void v5bCompletionDocMentionsKeyDeliverables() throws IOException {
        String doc = projectText(V5B_COMPLETION) + "\n" + projectText(V5B_SUMMARY);
        String lower = doc.toLowerCase(Locale.ROOT);
        assertThat(lower).contains(
                "dockerfile",
                "flyway",
                "readiness",
                "prometheus",
                "api key",
                "k8s",
                "helm",
                "release",
                "rollback");
    }

    @Test
    void summaryDocContainsInterviewTalkingPoints() throws IOException {
        String summary = projectText(V5B_SUMMARY);
        assertThat(summary).contains(
                "Interview Talking Points",
                "What does",
                "Is this production-ready",
                "production hardening");
    }

    // ---- D. Future / non-goals ----

    @Test
    void docsStateProductionDeploymentIsNotCompleted() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);
        assertThat(lower).contains("production deployment");
        assertThat(lower.contains("production deployment is not completed")
                || lower.contains("production deployment remains future")
                || lower.contains("production deployment not completed")).isTrue();
    }

    @Test
    void docsStateReleaseAutomationIsNotCompleted() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);
        assertThat(lower.contains("release automation is not completed")
                || lower.contains("release automation not completed")
                || lower.contains("release automation remains future")).isTrue();
    }

    @Test
    void docsStateRegistryPushIsNotCompleted() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);
        assertThat(lower.contains("registry push")
                || lower.contains("image registry push")
                || lower.contains("no image has been pushed")).isTrue();
    }

    @Test
    void docsStateSecretManagerIsNotCompleted() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);
        assertThat(lower.contains("secret manager is not completed")
                || lower.contains("secret manager not implemented")
                || lower.contains("secret manager not completed")).isTrue();
    }

    @Test
    void docsStateExternalIAMNotCompleted() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);
        assertThat(lower.contains("oauth2")
                || lower.contains("oidc")
                || lower.contains("external iam")).isTrue();
    }

    @Test
    void docsStateProductionMonitoringBackendNotCompleted() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);
        assertThat(lower.contains("production monitoring backend")
                || lower.contains("production monitoring is not completed")).isTrue();
    }

    @Test
    void docsStateSBOMSigningIsFuture() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);
        assertThat(lower.contains("sbom")
                || lower.contains("image signing")
                || lower.contains("provenance")).isTrue();
    }

    @Test
    void docsStateDefaultMavenGateDoesNotNeedDockerHelmK8s() throws IOException {
        String docs = combinedDocs();
        assertThat(docs).contains("mvn test");
        // Default gate does not require Docker/Helm/K8s
        assertThat(docs.contains("does not require")
                || docs.contains("does NOT require")
                || docs.contains("does not need")).isTrue();
    }

    // ---- E. Safety ----

    @Test
    void allV5BDocsDoNotContainRealSecretsLocalPathsOrOverclaims() throws IOException {
        List<String> allFiles = new java.util.ArrayList<>(ALL_V5B_DOCS);
        allFiles.addAll(ALL_B_COMPLETION_RECORDS);

        for (String path : allFiles) {
            assertSafeText(path, projectText(path));
        }
    }

    @Test
    void noFileClaimsFullProductionDeploymentCompleted() throws IOException {
        List<String> allFiles = new java.util.ArrayList<>(ALL_V5B_DOCS);
        allFiles.addAll(ALL_B_COMPLETION_RECORDS);

        for (String path : allFiles) {
            String text = projectText(path);
            assertThat(text).as(path).doesNotContain(
                    "full production deployment completed",
                    "full production operations completed",
                    "production deployment is completed");
        }
    }

    @Test
    void noFileClaimsAutomationOrIntegrationCompleted() throws IOException {
        List<String> allFiles = new java.util.ArrayList<>(ALL_V5B_DOCS);
        allFiles.addAll(ALL_B_COMPLETION_RECORDS);

        for (String path : allFiles) {
            String text = projectText(path);
            assertThat(text).as(path).doesNotContain(
                    "release automation is completed",
                    "rollback automation is completed",
                    "production monitoring backend is completed",
                    "真实退款已接入",
                    "真实换货已接入",
                    "真实补偿已接入",
                    "真实支付已接入",
                    "真实物流已接入");
        }
    }

    private static void assertSafeText(String path, String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        assertThat(lower).as(path).doesNotContain(
                "d:/", "d:\\", "c:/", "c:\\", "/users/", "/home/",
                "openai_api_key=", "dashscope_api_key=", "spring_ai_openai_api_key=",
                "password=prod", "password=production", "sk-");

        assertThat(text).as(path).doesNotContain(
                "真实退款已接入", "真实换货已接入", "真实补偿已接入",
                "真实支付已接入", "真实物流已接入");
    }

    private static String combinedDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : ALL_V5B_DOCS) {
            builder.append(projectText(path)).append('\n');
        }
        return builder.toString();
    }

    private static String projectText(String path) throws IOException {
        Path file = PROJECT_ROOT.resolve(path);
        assertThat(Files.exists(file)).as(path + " should exist").isTrue();
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
