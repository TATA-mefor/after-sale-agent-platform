package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class TracingCorrelationDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String BOUNDARY_DOC = "docs/deploy/OBSERVABILITY_TRACING_CORRELATION.md";

    private static final String COMPLETED_PLAN =
            "docs/exec-plans/completed/EXEC_PLAN_V5_B3_4_TRACING_CORRELATION_BOUNDARY.md";

    private static final List<String> V5_B3_4_DOCS = List.of(
            "README.md",
            "docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md",
            "docs/deploy/OBSERVABILITY_METRICS_FOUNDATION.md",
            "docs/deploy/OBSERVABILITY_PROMETHEUS_OPT_IN.md",
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
    void completionRecordExistsAndMarksV5B34Completed() throws IOException {
        String completedPlan = projectText(COMPLETED_PLAN);

        assertThat(completedPlan).contains(
                "Status: Completed",
                "Correlation ID Boundary",
                "Request ID Boundary",
                "MDC Boundary",
                "HTTP Header Boundary",
                "Structured Logging Boundary",
                "AgentRun / ToolCallTrace / ExecutionTree Boundary",
                "Metrics Tag Boundary",
                "Secret Safety Boundary",
                "OpenTelemetry Boundary",
                "Production Tracing Boundary",
                "Runtime Non-change Boundary",
                "Default Offline Boundary",
                "TASK_COMPLETE");
    }

    @Test
    void statusDocsRecordV5B34CompletedAndFutureWorkPlanned() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "V5.B.3.4 tracing / correlation boundary completed",
                "V5.B.3.5 observability docs + completion record completed",
                "V5.B.4 planned",
                BOUNDARY_DOC,
                COMPLETED_PLAN);
    }

    @Test
    void boundaryDocDescribesHeadersAndMdcFields() throws IOException {
        String boundaryDoc = projectText(BOUNDARY_DOC);

        assertThat(boundaryDoc).contains(
                "X-Correlation-Id",
                "X-Request-Id",
                "correlationId",
                "requestId",
                "MDC",
                "safe characters",
                "128",
                "Unsafe header values",
                "finally");
    }

    @Test
    void actuatorAndMetricsBoundariesAreDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "health-only",
                "/actuator/health",
                "/actuator/health/liveness",
                "/actuator/health/readiness",
                "/actuator/prometheus",
                "/actuator/metrics",
                "unavailable by default",
                "must not be used as Micrometer tags",
                "low-cardinality",
                "Metric tags must not include `correlationId`, `requestId`");
    }

    @Test
    void agentRuntimeAndAuditBoundariesAreDocumentedAsUnchanged() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "ToolRegistry remains the Agent tool execution entry",
                "AgentRun state transitions are unchanged",
                "ToolCallTrace schema and write behavior are unchanged",
                "Workspace evidence logic is unchanged",
                "Execution Tree runtime is unchanged",
                "Approval behavior is unchanged",
                "does not create Ticket, AgentRun, ToolCallTrace, Workspace, Approval, or Execution Tree records");
    }

    @Test
    void openTelemetryDistributedTracingAndProductionTracingRemainFutureWork() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);

        assertThat(docs).contains(
                "not OpenTelemetry",
                "not distributed tracing",
                "not production tracing",
                "W3C `traceparent`",
                "cross-service propagation",
                "Jaeger",
                "Zipkin",
                "future / opt-in");
        assertThat(lower).doesNotContain(
                "opentelemetry completed",
                "distributed tracing completed",
                "production tracing completed",
                "production monitoring completed",
                "production deployment completed",
                "production auth completed");
    }

    @Test
    void defaultOfflineBoundaryAndValidationCommandsAreDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "real LLM",
                "API Key",
                "PostgreSQL",
                "PGvector",
                "Docker",
                "MySQL",
                "Redis",
                "real embedding provider",
                "Spring AI live provider calls",
                "external network",
                "mvn test -Dtest=CorrelationIdsTest",
                "mvn test -Dtest=CorrelationIdFilterBoundaryTest",
                "mvn test -Dtest=CorrelationObservabilityBoundaryTest",
                "mvn test -Dtest=TracingCorrelationDocsTest",
                "mvn test",
                "mvn checkstyle:check",
                "mvn spotbugs:check",
                "mvn test -Dtest=ArchitectureTest");
    }

    @Test
    void docsDoNotContainSecretsLocalPathsOrOverclaims() throws IOException {
        for (String path : V5_B3_4_DOCS) {
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
                "correlation id as metric tag",
                "request id as metric tag",
                "opentelemetry completed",
                "distributed tracing completed",
                "production tracing completed",
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
        for (String path : V5_B3_4_DOCS) {
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
