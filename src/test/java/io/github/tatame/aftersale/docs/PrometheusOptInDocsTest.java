package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class PrometheusOptInDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String BOUNDARY_DOC = "docs/deploy/OBSERVABILITY_PROMETHEUS_OPT_IN.md";

    private static final String COMPLETED_PLAN =
            "docs/exec-plans/completed/EXEC_PLAN_V5_B3_3_PROMETHEUS_OPT_IN_EXPOSURE.md";

    private static final List<String> V5_B3_3_DOCS = List.of(
            "README.md",
            "docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md",
            "docs/deploy/OBSERVABILITY_METRICS_FOUNDATION.md",
            "docs/deploy/OBSERVABILITY_READINESS_LIVENESS.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "docs/decisions/DECISION_PROJECT_REVIEW_OBSERVABILITY_HARDENING.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            "version-updates/V5_A_RAG_PRODUCTION_PATH_SUMMARY.md",
            BOUNDARY_DOC,
            COMPLETED_PLAN);

    @Test
    void completionRecordExistsAndMarksV5B33Completed() throws IOException {
        String completedPlan = projectText(COMPLETED_PLAN);

        assertThat(completedPlan).contains(
                "Status: Completed",
                "Prometheus Registry Boundary",
                "Opt-in Profile Boundary",
                "Default Exposure Boundary",
                "Actuator Endpoint Boundary",
                "Metrics Policy Boundary",
                "Secret Safety Boundary",
                "OpenTelemetry Boundary",
                "Production Monitoring Boundary",
                "Runtime Non-change Boundary",
                "Default Offline Boundary",
                "TASK_COMPLETE");
    }

    @Test
    void statusDocsRecordV5B33CompletedAndFutureWorkPlanned() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "V5.B.3.3 Prometheus opt-in exposure completed",
                "V5.B.3.4 tracing / correlation boundary completed",
                "V5.B.3.5 observability docs + completion record completed",
                "V5.B.4 planned",
                BOUNDARY_DOC,
                COMPLETED_PLAN);
    }

    @Test
    void readmeLinksPrometheusDocs() throws IOException {
        String readme = projectText("README.md");

        assertThat(readme).contains(
                "Prometheus",
                "Micrometer",
                "Observability");
    }

    @Test
    void actuatorExposureBoundaryIsDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "observability-prometheus",
                "/actuator/health",
                "/actuator/health/liveness",
                "/actuator/health/readiness",
                "/actuator/prometheus",
                "/actuator/metrics",
                "/actuator/env",
                "/actuator/beans",
                "/actuator/configprops",
                "/actuator/heapdump",
                "/actuator/threaddump",
                "health-only",
                "unavailable by default",
                "opt-in");
    }

    @Test
    void metricsPolicyAndSecretSafetyAreDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "low-cardinality",
                "Metric tags must not include API keys",
                "passwords",
                "tokens",
                "local paths",
                "raw prompts",
                "raw provider responses",
                "raw user messages",
                "raw policy snippets",
                "JDBC URLs");
    }

    @Test
    void validationCommandsAreDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "mvn test -Dtest=PrometheusOptInExposureTest",
                "mvn test -Dtest=PrometheusOptInDocsTest",
                "mvn test",
                "mvn checkstyle:check",
                "mvn spotbugs:check",
                "mvn test -Dtest=ArchitectureTest");
    }

    @Test
    void openTelemetryAndProductionMonitoringRemainFutureWork() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);

        assertThat(docs).contains(
                "OpenTelemetry",
                "distributed tracing",
                "cross-service",
                "Grafana",
                "production monitoring",
                "future");
        assertThat(lower).doesNotContain(
                "opentelemetry completed",
                "distributed tracing completed",
                "production monitoring completed",
                "production deployment completed",
                "production auth completed");
    }

    @Test
    void defaultOfflineBoundaryIsDocumented() throws IOException {
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
                "external network");
    }

    @Test
    void docsDoNotContainSecretsLocalPathsOrOverclaims() throws IOException {
        for (String path : V5_B3_3_DOCS) {
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
        for (String path : V5_B3_3_DOCS) {
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
