package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class AgentRunStatusDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String COMPLETED_PLAN =
            "version-updates/"
                    + "EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE3_3_AGENT_RUN_STATUS_READ.md";

    private static final List<String> STAGE_THREE_THREE_DOCS = List.of(
            "README.md",
            "docs/api/OPENAPI.md",
            "docs/decisions/DECISION_PROJECT_REVIEW_API_COMPLETENESS.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "docs/quality/QUALITY_SCORE.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            COMPLETED_PLAN);

    @Test
    void agentRunStatusCompletionRecordExistsAndIsLinked() throws IOException {
        String readme = projectText("README.md");
        String completion = projectText(COMPLETED_PLAN);
        String activePlan = projectText("version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md");

        assertThat(completion).contains(
                "Status: Completed",
                "AgentRun Read Boundary",
                "Status Polling Boundary",
                "Trace / Execution Tree Boundary",
                "API Compatibility Boundary",
                "ToolRegistry / Planner Boundary",
                "Default Offline Boundary",
                "TASK_COMPLETE");
        assertThat(readme).contains(COMPLETED_PLAN);
        assertThat(activePlan).contains(
                "状态：阶段 0-3.4 已完成",
                "阶段 3.3：AgentRun get/status polling read model",
                "状态：已完成",
                COMPLETED_PLAN);
    }

    @Test
    void docsRecordAgentRunStatusEndpointAndSafeFields() throws IOException {
        String docs = combinedStageThreeThreeDocs();

        assertThat(docs).contains(
                "Project Review Correction Stage 3.3 (completed)",
                "AgentRun Status Read Validation",
                "AgentRun get/status polling",
                "`GET /api/agent-runs/{runId}`",
                "read-only status",
                "`runId`",
                "`ticketId`",
                "`status`",
                "`startedAt`",
                "`completedAt`",
                "`finalSummary`",
                "`failureSummary`",
                "`traceAvailable`",
                "`executionTreeAvailable`",
                "`traceUrl`",
                "`executionTreeUrl`");
    }

    @Test
    void docsPreserveToolRegistryTraceExecutionTreeAndRuntimeBoundaries() throws IOException {
        String docs = combinedStageThreeThreeDocs();

        assertThat(docs).contains(
                "does not run Planner",
                "does not call ToolRegistry",
                "does not write ToolCallTrace",
                "does not modify Ticket",
                "does not inline",
                "ToolCallTrace details remain available only through",
                "Execution Tree details remain available only through",
                "`search_aftersale_policy`",
                "LOW-risk read-only ToolRegistry tool",
                "RAG evidence",
                "policy evidence");
        assertThat(docs).contains(
                "不执行 Planner",
                "不调用 ToolRegistry",
                "不写 ToolCallTrace",
                "不修改 Ticket",
                "不修改 ToolRegistry、Planner、RAG runtime");
    }

    @Test
    void laterApiWorkRemainsFuture() throws IOException {
        String docs = combinedStageThreeThreeDocs();
        String lower = docs.toLowerCase(Locale.ROOT);

        assertThat(docs).contains(
                "Stage 3.4",
                "async AgentRun",
                "SSE / WebSocket",
                "batch API",
                "production auth / RBAC",
                "planned",
                "future");
        assertThat(lower).doesNotContain(
                "production-grade async agentrun completed",
                "async agentrun completed",
                "sse completed",
                "websocket completed",
                "batch api completed",
                "production auth completed",
                "rbac completed",
                "public rag http endpoint completed",
                "production api hardening completed");
    }

    @Test
    void docsDoNotContainSecretsLocalPathsOrProductionOverclaims() throws IOException {
        for (String path : STAGE_THREE_THREE_DOCS) {
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
                "openai_api_key=",
                "dashscope_api_key=",
                "spring_ai_openai_api_key=",
                "password=prod",
                "password=production",
                "token=",
                "secret=",
                "sk-",
                "real refund integration completed",
                "real exchange integration completed",
                "real payment integration completed",
                "real logistics integration completed",
                "production deployment completed",
                "production monitoring completed",
                "production auth completed",
                "真实退款已接入",
                "真实换货已接入",
                "真实支付已接入",
                "真实物流已接入");
    }

    private static String combinedStageThreeThreeDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : STAGE_THREE_THREE_DOCS) {
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
