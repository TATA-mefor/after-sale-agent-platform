package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ReleaseRollbackFoundationDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String FOUNDATION_DOC = "docs/deploy/RELEASE_ROLLBACK_FOUNDATION.md";
    private static final String RELEASE_TEMPLATE =
            "docs/deploy/release-templates/RELEASE_CHECKLIST_TEMPLATE.md";
    private static final String ROLLBACK_TEMPLATE =
            "docs/deploy/release-templates/ROLLBACK_CHECKLIST_TEMPLATE.md";
    private static final String CHANGE_TEMPLATE =
            "docs/deploy/release-templates/CHANGE_RECORD_TEMPLATE.md";
    private static final String COMPLETED_PLAN =
            "docs/exec-plans/completed/EXEC_PLAN_V5_B4_4_RELEASE_ROLLBACK_FOUNDATION.md";

    private static final List<String> ALL_RELEASE_DOCS = List.of(
            "README.md",
            FOUNDATION_DOC,
            COMPLETED_PLAN,
            "docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "docs/deploy/K8S_HELM_FOUNDATION.md",
            "deploy/k8s/README.md",
            "deploy/helm/after-sale-agent-platform/README.md",
            "docs/deploy/AUTH_RUNTIME_FOUNDATION.md",
            "docs/deploy/AUTH_RBAC_BOUNDARY.md",
            "docs/deploy/OBSERVABILITY_DOCS_COMPLETION.md",
            "docs/deploy/OBSERVABILITY_PROMETHEUS_OPT_IN.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md");

    // ---- A. File existence ----

    @Test
    void allReleaseAndTemplateFilesExist() throws IOException {
        assertThat(PROJECT_ROOT.resolve(FOUNDATION_DOC)).exists();
        assertThat(PROJECT_ROOT.resolve(RELEASE_TEMPLATE)).exists();
        assertThat(PROJECT_ROOT.resolve(ROLLBACK_TEMPLATE)).exists();
        assertThat(PROJECT_ROOT.resolve(CHANGE_TEMPLATE)).exists();
        assertThat(PROJECT_ROOT.resolve(COMPLETED_PLAN)).exists();
        assertThat(PROJECT_ROOT.resolve("docs/quality/VALIDATION_COMMANDS.md")).exists();
        assertThat(PROJECT_ROOT.resolve("docs/quality/QUALITY_SCORE.md")).exists();
    }

    @Test
    void completionRecordContainsTaskComplete() throws IOException {
        String completedPlan = projectText(COMPLETED_PLAN);
        assertThat(completedPlan).contains("Status: Completed", "TASK_COMPLETE");
    }

    // ---- B. Docs status ----

    @Test
    void readmeMentionsReleaseRollbackFoundation() throws IOException {
        String readme = projectText("README.md");
        assertThat(readme).contains(
                "[Release / Rollback Foundation](" + FOUNDATION_DOC + ")",
                "Release / Rollback");
    }

    @Test
    void deploymentRoadmapMarksB44CompletedAndB4ScopeCompleted() throws IOException {
        String roadmap = projectText("docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md");
        String lower = roadmap.toLowerCase(Locale.ROOT);
        assertThat(roadmap).contains("V5.B.4.4 Release / Rollback Foundation completed");
        assertThat(lower).contains("current scope completed");
        // Roadmap explicitly states production deployment remains future work
        assertThat(lower).contains("production deployment");
    }

    @Test
    void qualityScoreMentionsReleaseRollbackFoundation() throws IOException {
        String quality = projectText("docs/quality/QUALITY_SCORE.md");
        assertThat(quality).contains(
                "V5.B.4.4 Release / Rollback Foundation",
                "Release checklist",
                "Rollback runbook");
    }

    @Test
    void validationDocsMentionReleaseRollbackTest() throws IOException {
        String validation = projectText("docs/quality/VALIDATION_COMMANDS.md");
        assertThat(validation).contains(
                "mvn test -Dtest=ReleaseRollbackFoundationDocsTest",
                "V5.B.4.4 Release / Rollback Foundation Validation");
    }

    // ---- C. Release policy ----

    @Test
    void foundationDocContainsReleaseChecklistAndRollbackRunbook() throws IOException {
        String doc = projectText(FOUNDATION_DOC);
        assertThat(doc).contains(
                "Release Checklist",
                "Rollback Strategy",
                "Rollback Trigger Matrix",
                "Image Tag Policy");
    }

    @Test
    void foundationDocContainsImageTagPolicy() throws IOException {
        String doc = projectText(FOUNDATION_DOC);
        assertThat(doc).contains(
                "immutable",
                "Image Tag Policy");
        // Prohibits latest tag for production
        assertThat(doc.toLowerCase(Locale.ROOT)).contains("`latest`");
    }

    @Test
    void foundationDocContainsReviewBoundaries() throws IOException {
        String doc = projectText(FOUNDATION_DOC);
        assertThat(doc).contains(
                "Helm Release Review",
                "Config / Secret Review",
                "Migration Review",
                "Auth / Security Review",
                "Observability Review");
    }

    @Test
    void foundationDocContainsPostReleaseVerification() throws IOException {
        String doc = projectText(FOUNDATION_DOC);
        assertThat(doc).contains(
                "Post-release Verification",
                "/actuator/health",
                "/actuator/health/liveness",
                "/actuator/health/readiness");
    }

    @Test
    void foundationDocContainsRollbackTriggerMatrix() throws IOException {
        String doc = projectText(FOUNDATION_DOC);
        assertThat(doc).contains(
                "Rollback Trigger Matrix",
                "Startup failure",
                "Readiness failure",
                "Liveness restart loop",
                "Auth rejection spike",
                "Migration failure",
                "Configuration error",
                "Secret missing");
    }

    @Test
    void releaseTemplateUsesPlaceholdersNotRealValues() throws IOException {
        String template = projectText(RELEASE_TEMPLATE);
        assertThat(template).contains("REPLACE_WITH_");
        assertThat(template.toLowerCase(Locale.ROOT)).doesNotContain(
                "sk-", "d:/", "c:/", "/users/", "/home/");
    }

    @Test
    void rollbackTemplateUsesPlaceholdersNotRealValues() throws IOException {
        String template = projectText(ROLLBACK_TEMPLATE);
        assertThat(template).contains("REPLACE_WITH_");
        assertThat(template.toLowerCase(Locale.ROOT)).doesNotContain(
                "sk-", "d:/", "c:/", "/users/", "/home/");
    }

    @Test
    void changeRecordTemplateUsesPlaceholdersNotRealValues() throws IOException {
        String template = projectText(CHANGE_TEMPLATE);
        assertThat(template).contains("REPLACE_WITH_");
        assertThat(template.toLowerCase(Locale.ROOT)).doesNotContain(
                "sk-", "d:/", "c:/", "/users/", "/home/");
    }

    // ---- D. Non-goals ----

    @Test
    void docsStateNoProductionDeploymentOrAutomationCompleted() throws IOException {
        String doc = projectText(FOUNDATION_DOC);
        String lower = doc.toLowerCase(Locale.ROOT);
        // Foundation doc states this is governance/runbook foundation, not production deployment or automation
        assertThat(lower).contains("not a production deployment");
        assertThat(lower).contains("release automation");
    }

    @Test
    void docsStateNoRegistryPushOrGitHubRelease() throws IOException {
        String docs = combinedDocs();
        assertThat(docs).contains(
                "no image has been pushed",
                "no GitHub release workflow");
    }

    @Test
    void docsStateNoRealHelmOrKubectlExecuted() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);
        assertThat(lower).contains(
                "no helm release",
                "no real release",
                "no kubectl apply");
    }

    @Test
    void docsStateDockerHelmKubectlAreOptional() throws IOException {
        String doc = projectText(FOUNDATION_DOC);
        assertThat(doc).contains("optional");
        assertThat(doc.toLowerCase(Locale.ROOT)).contains("not part of the default maven");
    }

    @Test
    void docsStateDefaultMavenGateDoesNotNeedDockerHelmK8s() throws IOException {
        String doc = projectText(FOUNDATION_DOC);
        assertThat(doc).contains("does NOT require Docker");
        // "Helm, kubectl" follows in the same sentence
        assertThat(doc).contains("Helm, kubectl");
    }

    @Test
    void docsStateNoSecretManagerCompleted() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);
        assertThat(lower.contains("no external secret manager")
                || lower.contains("secret manager")).isTrue();
    }

    // ---- E. Safety ----

    @Test
    void allReleaseDocsDoNotContainSecretsLocalPathsOrOverclaims() throws IOException {
        List<String> allFiles = new java.util.ArrayList<>(ALL_RELEASE_DOCS);
        allFiles.add(RELEASE_TEMPLATE);
        allFiles.add(ROLLBACK_TEMPLATE);
        allFiles.add(CHANGE_TEMPLATE);

        for (String path : allFiles) {
            assertSafeText(path, projectText(path));
        }
    }

    @Test
    void noFileClaimsProductionDeploymentOrAutomationCompleted() throws IOException {
        List<String> allFiles = new java.util.ArrayList<>(ALL_RELEASE_DOCS);
        allFiles.add(RELEASE_TEMPLATE);
        allFiles.add(ROLLBACK_TEMPLATE);
        allFiles.add(CHANGE_TEMPLATE);

        for (String path : allFiles) {
            String text = projectText(path);
            assertThat(text).as(path).doesNotContain(
                    "production deployment is completed",
                    "production deployment was completed",
                    "release automation is completed",
                    "rollback automation is completed",
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

        // Check for positive overclaims only
        assertThat(text).as(path).doesNotContain(
                "kubernetes deployment completed",
                "helm release completed",
                "真实退款已接入", "真实换货已接入", "真实补偿已接入",
                "真实支付已接入", "真实物流已接入");
    }

    private static String combinedDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : ALL_RELEASE_DOCS) {
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
