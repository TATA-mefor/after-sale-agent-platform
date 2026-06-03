package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ReadinessLivenessBoundaryDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String BOUNDARY_DOC = "docs/deploy/OBSERVABILITY_READINESS_LIVENESS.md";

    private static final String COMPLETED_PLAN =
            "docs/exec-plans/completed/EXEC_PLAN_V5_B3_1_READINESS_LIVENESS_BOUNDARY.md";

    private static final List<String> V5_B3_1_DOCS = List.of(
            "README.md",
            "docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            "version-updates/V5_A_RAG_PRODUCTION_PATH_SUMMARY.md",
            BOUNDARY_DOC,
            COMPLETED_PLAN);

    @Test
    void completionRecordExistsAndMarksV5B31Completed() throws IOException {
        String completedPlan = projectText(COMPLETED_PLAN);

        assertThat(completedPlan).contains(
                "Status: Completed",
                "Liveness Boundary",
                "Readiness Boundary",
                "Actuator Exposure Boundary",
                "Secret Safety Boundary",
                "Live Dependency Boundary",
                "Metrics / Tracing Boundary",
                "Runtime Non-change Boundary",
                "Default Offline Boundary",
                "TASK_COMPLETE");
    }

    @Test
    void statusDocsRecordV5B31CompletedAndFutureWorkPlanned() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "V5.B.3.1 Readiness / Liveness Boundary",
                "readiness / liveness actuator probe boundary completed",
                "V5.B.3.2 planned",
                "V5.B.3.3 planned",
                "V5.B.3.4 planned",
                "V5.B.4 planned",
                COMPLETED_PLAN,
                BOUNDARY_DOC);
    }

    @Test
    void actuatorProbeBoundaryIsDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "/actuator/health",
                "/actuator/health/liveness",
                "/actuator/health/readiness",
                "/actuator/env",
                "/actuator/beans",
                "/actuator/configprops",
                "/actuator/heapdump",
                "/actuator/threaddump",
                "/actuator/prometheus",
                "Actuator web exposure remains health-only",
                "health-only exposure",
                "unavailable by default");
    }

    @Test
    void readinessAndLivenessDoNotClaimLiveDependencyChecks() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "Liveness means the Spring Boot process and application lifecycle state",
                "Readiness means the application is basically ready to receive traffic",
                "default offline / local profile",
                "does not prove live PostgreSQL / PGvector connectivity",
                "real LLM",
                "real embedding provider",
                "Spring AI `VectorStore`",
                "external network",
                "future / opt-in");
    }

    @Test
    void metricsTracingAndProductionMonitoringRemainFutureWork() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);

        assertThat(docs).contains(
                "Micrometer business metrics",
                "Prometheus registry",
                "Grafana dashboards",
                "OpenTelemetry",
                "collector configuration",
                "production monitoring is not completed",
                "V5.B.3.2 planned");
        assertThat(lower).doesNotContain(
                "prometheus registry completed",
                "opentelemetry completed",
                "production monitoring completed");
    }

    @Test
    void validationCommandsAreDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "mvn test -Dtest=ReadinessLivenessBoundaryTest",
                "mvn test -Dtest=ReadinessLivenessBoundaryDocsTest",
                "mvn test",
                "mvn checkstyle:check",
                "mvn spotbugs:check",
                "mvn test -Dtest=ArchitectureTest");
    }

    @Test
    void docsDoNotContainSecretsLocalPathsOrOverclaims() throws IOException {
        for (String path : V5_B3_1_DOCS) {
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
                "env exposed by default",
                "beans exposed by default",
                "configprops exposed by default",
                "heapdump exposed by default",
                "threaddump exposed by default",
                "prometheus exposed by default",
                "prometheus registry completed",
                "opentelemetry completed",
                "production monitoring completed",
                "live dependency readiness completed",
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
        for (String path : V5_B3_1_DOCS) {
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
