package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ObservabilityDocsCompletionDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String COMPLETION_DOC = "docs/deploy/OBSERVABILITY_DOCS_COMPLETION.md";

    private static final String COMPLETED_PLAN =
            "docs/exec-plans/completed/EXEC_PLAN_V5_B3_5_OBSERVABILITY_DOCS_COMPLETION_RECORD.md";

    private static final List<String> V5_B3_5_DOCS = List.of(
            "README.md",
            "docs/decisions/DECISION_PROJECT_REVIEW_OBSERVABILITY_HARDENING.md",
            "docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md",
            "docs/deploy/OBSERVABILITY_READINESS_LIVENESS.md",
            "docs/deploy/OBSERVABILITY_METRICS_FOUNDATION.md",
            "docs/deploy/OBSERVABILITY_PROMETHEUS_OPT_IN.md",
            "docs/deploy/OBSERVABILITY_TRACING_CORRELATION.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            "version-updates/V5_A_RAG_PRODUCTION_PATH_SUMMARY.md",
            COMPLETION_DOC,
            COMPLETED_PLAN);

    @Test
    void observabilityCompletionDocsExistAndMarkCompleted() throws IOException {
        String completionDoc = projectText(COMPLETION_DOC);
        String completedPlan = projectText(COMPLETED_PLAN);

        assertThat(completionDoc).contains(
                "Status: Completed",
                "Current Observability Baseline",
                "Production Monitoring Roadmap Boundary",
                "Metrics Boundary",
                "Tracing Boundary",
                "Default Offline Boundary",
                "TASK_COMPLETE");
        assertThat(completedPlan).contains(
                "Status: Completed",
                "Observability Docs Boundary",
                "Production Monitoring Boundary",
                "OpenTelemetry / Distributed Tracing Boundary",
                "Runtime Non-change Boundary",
                "Default Offline Boundary",
                "TASK_COMPLETE");
    }

    @Test
    void readmeLinksCompletionDocsAndReleaseStatus() throws IOException {
        String readme = projectText("README.md");

        assertThat(readme).contains(
                "[Observability](docs/OBSERVABILITY.md)",
                "[Deployment Hardening Roadmap](docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md)");
        // V5.B.4 and production monitoring details are in the deployment roadmap
        assertThat(projectText("docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md"))
                .contains("V5.B.4", "production monitoring");
    }

    @Test
    void statusDocsRecordB35CompletedWithoutProductionMonitoringOverclaim() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "V5.B.3.1 Readiness / Liveness Boundary",
                "V5.B.3.2 Micrometer metrics foundation completed",
                "V5.B.3.3 Prometheus opt-in exposure completed",
                "V5.B.3.4 tracing / correlation boundary completed",
                "V5.B.3.5 observability docs + completion record completed",
                "V5.B.4 planned",
                "production monitoring backend",
                "future / opt-in");
    }

    @Test
    void observabilityRoadmapBoundaryKeepsProductionTracingAndLoggingFuture() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);

        assertThat(docs).contains(
                "OpenTelemetry",
                "distributed tracing",
                "log aggregation",
                "Grafana dashboards",
                "Prometheus scrape jobs",
                "alert rules",
                "production incident response runbooks");
        assertThat(lower).doesNotContain(
                "opentelemetry completed",
                "distributed tracing completed",
                "production monitoring completed",
                "production deployment completed",
                "production auth completed");
    }

    @Test
    void runtimeNonChangeAndDefaultOfflineBoundariesAreDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "does not add runtime observability behavior",
                "ToolRegistry execution semantics",
                "`search_aftersale_policy` runtime",
                "RAG retrieval algorithm",
                "ToolCallTrace schema",
                "Workspace evidence logic",
                "Execution Tree runtime",
                "real LLM",
                "API Key",
                "PostgreSQL",
                "PGvector",
                "Docker",
                "MySQL",
                "Redis",
                "real embedding provider",
                "external network");
    }

    @Test
    void validationCommandsAreDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "mvn test -Dtest=ObservabilityDocsCompletionDocsTest",
                "mvn test",
                "mvn checkstyle:check",
                "mvn spotbugs:check",
                "mvn test -Dtest=ArchitectureTest");
    }

    @Test
    void docsDoNotContainSecretsLocalPathsOrProductionOverclaims() throws IOException {
        for (String path : V5_B3_5_DOCS) {
            assertSafeText(path, projectText(path));
        }
    }

    private static void assertSafeText(String path, String text) {
        String lower = text.toLowerCase(Locale.ROOT);

        assertThat(lower).as(path).doesNotContain(
                "d:/",
                "d:\\",
                "c:/",
                "c:\\",
                "/users/",
                "/home/",
                "openai_api_key=",
                "dashscope_api_key=",
                "spring_ai_openai_api_key=",
                "password=prod",
                "password=production",
                "token=",
                "secret=",
                "sk-",
                "opentelemetry completed",
                "distributed tracing completed",
                "production monitoring completed",
                "production deployment completed",
                "production auth completed",
                "真实退款已接入",
                "真实换货已接入",
                "真实补偿已接入",
                "真实支付已接入",
                "真实物流已接入");
    }

    private static String combinedDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : V5_B3_5_DOCS) {
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
