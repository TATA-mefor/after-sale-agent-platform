package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ObservabilityHardeningDecisionDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String DECISION_DOC =
            "docs/decisions/DECISION_PROJECT_REVIEW_OBSERVABILITY_HARDENING.md";

    private static final String COMPLETED_PLAN =
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE2_OBSERVABILITY_HARDENING.md";

    private static final List<String> STAGE_TWO_DOCS = List.of(
            "README.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "docs/quality/QUALITY_SCORE.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            DECISION_DOC,
            COMPLETED_PLAN);

    @Test
    void observabilityDecisionAndCompletionRecordExistAndAreLinked() throws IOException {
        String readme = projectText("README.md");
        String validation = projectText("docs/quality/VALIDATION_COMMANDS.md");
        String remediation = projectText("docs/quality/PROJECT_REMEDIATION_PLAN.md");
        String quality = projectText("docs/quality/QUALITY_SCORE.md");
        String activePlan = projectText("version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md");
        String completedPlan = projectText(COMPLETED_PLAN);

        assertThat(projectText(DECISION_DOC)).contains("Status: Completed");
        assertThat(completedPlan).contains("Status: Completed", "TASK_COMPLETE");
        assertThat(readme).contains(DECISION_DOC, COMPLETED_PLAN);
        assertThat(validation).contains("ObservabilityHardeningDecisionDocsTest", DECISION_DOC);
        assertThat(remediation).contains("阶段 2：已完成", DECISION_DOC);
        assertThat(quality).contains("Project Review Correction Stage 2 (completed)");
        assertThat(activePlan).contains("阶段 2：可观测性加固方案", "状态：已完成", DECISION_DOC, COMPLETED_PLAN);
    }

    @Test
    void decisionRecordsCurrentBaselineAndKnownGaps() throws IOException {
        String decision = projectText(DECISION_DOC);

        assertThat(decision).contains(
                "MDC / structured logging",
                "X-Request-Id",
                "ToolCallTrace",
                "ApprovalRequest",
                "Execution Tree",
                "/actuator/health",
                "RAG search",
                "vector-store",
                "embedding",
                "ingestion",
                "offline RAG evaluation metrics");
        assertThat(decision).contains(
                "没有 Prometheus registry",
                "没有 Grafana dashboard",
                "没有 OpenTelemetry tracing",
                "没有 collector",
                "没有跨服务 trace-id 传播",
                "没有 provider latency / cost metrics");
    }

    @Test
    void metricsAndTracingRemainFutureOptInNotCompletedRuntime() throws IOException {
        String docs = combinedStageTwoDocs();
        String lower = docs.toLowerCase();

        assertThat(docs).contains(
                "agent_run_total",
                "tool_call_total",
                "rag_search_total",
                "llm_provider_latency_seconds",
                "embedding_provider_latency_seconds",
                "future / opt-in",
                "MDC-only",
                "OpenTelemetry 是 future / opt-in path",
                "不实现 Micrometer instrumentation");
        assertThat(lower).doesNotContain(
                "prometheus completed",
                "grafana completed",
                "opentelemetry completed",
                "collector completed",
                "metrics dashboard completed",
                "production monitoring completed");
    }

    @Test
    void actuatorExposureAndHealthBoundaryStaySafe() throws IOException {
        String decision = projectText(DECISION_DOC);
        String validation = projectText("docs/quality/VALIDATION_COMMANDS.md");

        assertThat(decision).contains(
                "默认只暴露 `/actuator/health`",
                "不默认暴露 `/actuator/env`",
                "不默认暴露 `/actuator/beans`",
                "不默认暴露 `/actuator/configprops`",
                "不默认暴露 `/actuator/heapdump`",
                "不默认暴露 `/actuator/threaddump`",
                "不默认暴露 `/actuator/prometheus`");
        assertThat(decision).contains(
                "不调用真实 LLM",
                "不调用真实 embedding provider",
                "不连接 PostgreSQL / PGvector",
                "不调用 Spring AI `VectorStore`",
                "不调用 ToolRegistry",
                "不创建 AgentRun",
                "不写 ToolCallTrace",
                "health details 必须 sanitize");
        assertThat(validation).contains("默认 actuator exposure 继续只包含 `/actuator/health`");
        assertThat(validation).contains("敏感 actuator endpoints 如 env、beans、configprops、heapdump、threaddump、prometheus 不默认暴露");
    }

    @Test
    void defaultOfflineBoundaryDoesNotRequireMonitoringInfrastructure() throws IOException {
        String docs = combinedStageTwoDocs();

        assertThat(docs).contains(
                "默认验证仍不需要",
                "real LLM",
                "API Key",
                "PostgreSQL",
                "PGvector",
                "Docker",
                "MySQL",
                "Redis",
                "external network",
                "Prometheus",
                "Grafana",
                "OpenTelemetry collector",
                "external logging platform",
                "Spring AI live provider calls");
    }

    @Test
    void secretSafetyAndNoLocalPathBoundaryAreDocumented() throws IOException {
        String decision = projectText(DECISION_DOC);

        assertThat(decision).contains(
                "API keys",
                "database passwords",
                "tokens",
                "full prompts",
                "raw provider responses",
                "raw dataset paths",
                "local absolute paths",
                "customer private data",
                "metrics label");
        for (String path : STAGE_TWO_DOCS) {
            assertSafeText(path, projectText(path));
        }
    }

    private static void assertSafeText(String path, String text) {
        String lower = text.toLowerCase();

        assertThat(lower).as(path).doesNotContain(
                "d:/",
                "d:\\",
                "c:/",
                "c:\\",
                "/users/",
                "openai_api_key=",
                "dashscope_api_key=",
                "spring_ai_openai_api_key=",
                "password=prod",
                "password=production",
                "token=",
                "secret=",
                "sk-");
    }

    private static String combinedStageTwoDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : STAGE_TWO_DOCS) {
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
