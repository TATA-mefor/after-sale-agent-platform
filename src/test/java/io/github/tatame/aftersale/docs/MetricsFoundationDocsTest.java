package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class MetricsFoundationDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String BOUNDARY_DOC = "docs/deploy/OBSERVABILITY_METRICS_FOUNDATION.md";

    private static final String COMPLETED_PLAN =
            "docs/exec-plans/completed/EXEC_PLAN_V5_B3_2_MICROMETER_METRICS_FOUNDATION.md";

    private static final List<String> V5_B3_2_DOCS = List.of(
            "README.md",
            "docs/deploy/CONFIG_SECRET_MIGRATION_PLAN.md",
            "docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md",
            "docs/deploy/MIGRATION_FOUNDATION.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "docs/decisions/DECISION_V5_B2_CONFIG_SECRET_MIGRATION.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            "version-updates/V5_A_RAG_PRODUCTION_PATH_SUMMARY.md",
            BOUNDARY_DOC,
            COMPLETED_PLAN);

    @Test
    void completionRecordExistsAndMarksV5B32Completed() throws IOException {
        String completedPlan = projectText(COMPLETED_PLAN);

        assertThat(completedPlan).contains(
                "Status: Completed",
                "Micrometer Foundation Boundary",
                "Metric Naming Boundary",
                "Low-cardinality Tag Boundary",
                "AgentRun Metrics Boundary",
                "ToolCall Metrics Boundary",
                "Approval Metrics Boundary",
                "RAG Search Metrics Boundary",
                "Provider Metrics Boundary",
                "Actuator Exposure Boundary",
                "Prometheus / OpenTelemetry Boundary",
                "Default Offline Boundary",
                "TASK_COMPLETE");
    }

    @Test
    void statusDocsRecordV5B32CompletedAndFutureWorkPlanned() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "V5.B.3.2 Micrometer metrics foundation completed",
                "V5.B.3.3 planned",
                "V5.B.3.4 planned",
                "V5.B.4 planned",
                BOUNDARY_DOC,
                COMPLETED_PLAN);
    }

    @Test
    void readmeLinksMetricsDocs() throws IOException {
        String readme = projectText("README.md");

        assertThat(readme).contains(
                "[V5.B.3.2 Micrometer Metrics Foundation](" + BOUNDARY_DOC + ")",
                "[V5.B.3.2 Completion Record](" + COMPLETED_PLAN + ")");
    }

    @Test
    void metricNamesAndLowCardinalityTagsAreDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "aftersale.agent.run.total",
                "aftersale.agent.run.duration",
                "aftersale.tool.call.total",
                "aftersale.tool.call.duration",
                "aftersale.approval.request.total",
                "aftersale.approval.decision.total",
                "aftersale.rag.search.total",
                "aftersale.rag.search.duration",
                "aftersale.provider.call.total",
                "aftersale.provider.call.duration",
                "low-cardinality",
                "component",
                "operation",
                "outcome",
                "retrieval_mode",
                "provider_type",
                "approval_decision",
                "sanitized to `unknown`");
    }

    @Test
    void actuatorMetricsAndSensitiveEndpointsRemainUnavailableByDefault() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "Actuator web exposure remains health-only",
                "/actuator/metrics",
                "/actuator/prometheus",
                "/actuator/env",
                "/actuator/beans",
                "/actuator/configprops",
                "/actuator/heapdump",
                "/actuator/threaddump",
                "unavailable by default");
    }

    @Test
    void prometheusOpenTelemetryAndProductionMonitoringRemainFutureWork() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);

        assertThat(docs).contains(
                "Prometheus registry",
                "OpenTelemetry",
                "Grafana dashboard",
                "production monitoring backend",
                "planned / future work");
        assertThat(lower).doesNotContain(
                "prometheus registry completed",
                "opentelemetry completed",
                "production monitoring completed",
                "production auth completed",
                "production deployment completed");
    }

    @Test
    void validationCommandsAreDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "mvn test -Dtest=ApplicationMetricsRecorderTest",
                "mvn test -Dtest=MetricsFoundationBoundaryTest",
                "mvn test -Dtest=MetricsFoundationDocsTest",
                "mvn test",
                "mvn checkstyle:check",
                "mvn spotbugs:check",
                "mvn test -Dtest=ArchitectureTest");
    }

    @Test
    void defaultOfflineAndProviderBoundariesAreDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "Default Offline Boundary",
                "real LLM",
                "API Key",
                "PostgreSQL",
                "PGvector",
                "Docker",
                "MySQL",
                "Redis",
                "real embedding provider",
                "Spring AI live",
                "external network",
                "Provider metrics",
                "does not call real LLMs",
                "does not call real embedding providers");
    }

    @Test
    void docsDoNotContainSecretsLocalPathsOrProductionOverclaims() throws IOException {
        for (String path : V5_B3_2_DOCS) {
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
                "metrics exposed by default",
                "prometheus exposed by default",
                "prometheus registry completed",
                "opentelemetry completed",
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
        for (String path : V5_B3_2_DOCS) {
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
