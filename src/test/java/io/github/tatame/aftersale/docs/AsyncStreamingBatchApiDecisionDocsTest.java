package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class AsyncStreamingBatchApiDecisionDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String DECISION_DOC =
            "docs/decisions/DECISION_PROJECT_REVIEW_ASYNC_STREAMING_BATCH_API.md";

    private static final String COMPLETED_PLAN =
            "docs/exec-plans/completed/"
                    + "EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE3_4_ASYNC_STREAMING_BATCH_EVALUATION.md";

    private static final List<String> STAGE_THREE_FOUR_DOCS = List.of(
            "README.md",
            "docs/api/OPENAPI.md",
            "docs/decisions/DECISION_PROJECT_REVIEW_API_COMPLETENESS.md",
            DECISION_DOC,
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/exec-plans/active/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            COMPLETED_PLAN);

    @Test
    void decisionAndCompletionRecordExistAndAreLinked() throws IOException {
        String decision = projectText(DECISION_DOC);
        String completion = projectText(COMPLETED_PLAN);
        String readme = projectText("README.md");
        String openApiDocs = projectText("docs/api/OPENAPI.md");
        String apiCompleteness = projectText("docs/decisions/DECISION_PROJECT_REVIEW_API_COMPLETENESS.md");
        String remediation = projectText("docs/quality/PROJECT_REMEDIATION_PLAN.md");
        String validation = projectText("docs/quality/VALIDATION_COMMANDS.md");
        String quality = projectText("docs/quality/QUALITY_SCORE.md");
        String activePlan = projectText("docs/exec-plans/active/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md");

        assertThat(decision).contains(
                "Status: Completed",
                "Async AgentRun Evaluation",
                "Status Polling Evaluation",
                "SSE / WebSocket Streaming Evaluation",
                "Batch API Evaluation",
                "Cancel / Retry Evaluation",
                "AgentRun List Pagination Evaluation",
                "Security / Auth Boundary",
                "ToolRegistry / Planner Boundary",
                "TASK_COMPLETE");
        assertThat(completion).contains("Status: Completed", "TASK_COMPLETE");
        assertThat(readme).contains(DECISION_DOC, COMPLETED_PLAN);
        assertThat(openApiDocs).contains(DECISION_DOC, "Stage 3.4 evaluates async AgentRun");
        assertThat(apiCompleteness).contains(DECISION_DOC, "阶段 3.4 已完成");
        assertThat(remediation).contains("阶段 3.4：已完成", DECISION_DOC);
        assertThat(validation).contains("AsyncStreamingBatchApiDecisionDocsTest", DECISION_DOC);
        assertThat(quality).contains("Project Review Correction Stage 3.4 (completed)", DECISION_DOC);
        assertThat(activePlan).contains("状态：阶段 0-3.4 已完成", DECISION_DOC, COMPLETED_PLAN);
    }

    @Test
    void baselineApiSurfaceAndToolBoundariesAreDocumented() throws IOException {
        String docs = combinedStageThreeFourDocs();

        assertThat(docs).contains(
                "Ticket: create / get / list with bounded pagination",
                "AgentRun: create/start for a ticket",
                "`GET /api/agent-runs/{runId}` read-only status polling",
                "ToolCallTrace read-only view",
                "Execution Tree: read-only explanation view",
                "Approval: pending / get / approve / reject",
                "`search_aftersale_policy`",
                "LOW-risk read-only ToolRegistry tool",
                "不是 public RAG HTTP endpoint",
                "RAG evidence",
                "policy evidence");
    }

    @Test
    void futureRuntimeBoundariesAreExplicit() throws IOException {
        String docs = combinedStageThreeFourDocs();
        String lower = docs.toLowerCase(Locale.ROOT);

        assertThat(docs).contains(
                "保留当前 synchronous create/start",
                "status polling 作为当前安全路径",
                "不在本阶段实现 async AgentRun runtime",
                "不在本阶段实现 SSE / WebSocket streaming",
                "不在本阶段实现 batch API",
                "不在本阶段实现 cancel / retry API",
                "AgentRun list pagination 作为后续 read-only API 候选",
                "production auth / RBAC",
                "idempotency",
                "rate limit",
                "partial failure model");
        assertThat(lower).doesNotContain(
                "async agentrun completed",
                "sse completed",
                "websocket completed",
                "batch api completed",
                "cancel api completed",
                "retry api completed",
                "agentrun list pagination completed",
                "production api hardening completed",
                "production auth completed",
                "rbac completed");
    }

    @Test
    void streamingBatchSecurityAndApprovalBoundariesAreDocumented() throws IOException {
        String docs = combinedStageThreeFourDocs();

        assertThat(docs).contains(
                "streaming 不得暴露 raw prompt",
                "raw LLM response",
                "secrets",
                "full tool output",
                "完整 evidence chunk",
                "Batch API 不得让 LLM 直接执行工具",
                "不得绕过 ToolRegistry",
                "不得执行真实退款",
                "production auth / RBAC 是 streaming",
                "高风险动作",
                "Approval",
                "ToolRegistry 仍是 Agent tool execution entry");
    }

    @Test
    void runtimeNonChangeAndDefaultOfflineBoundariesAreDocumented() throws IOException {
        String docs = combinedStageThreeFourDocs();

        assertThat(docs).contains(
                "Stage 3.4 changes docs and docs harness tests only",
                "does not modify `src/main/java`",
                "不启动应用",
                "不调用 HTTP",
                "不连接数据库",
                "real LLM",
                "API Key",
                "PostgreSQL",
                "PGvector",
                "Docker",
                "MySQL",
                "Redis",
                "external network",
                "queue",
                "streaming server");
    }

    @Test
    void docsDoNotContainSecretsLocalPathsOrProductionOverclaims() throws IOException {
        for (String path : STAGE_THREE_FOUR_DOCS) {
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
                "real refund integration completed",
                "real exchange integration completed",
                "real payment integration completed",
                "real logistics integration completed",
                "real refund completed",
                "real exchange completed",
                "production deployment completed",
                "production monitoring completed",
                "production auth completed",
                "async agentrun runtime completed",
                "sse runtime completed",
                "websocket runtime completed",
                "batch api runtime completed",
                "真实退款已接入",
                "真实换货已接入",
                "真实支付已接入",
                "真实物流已接入");
    }

    private static String combinedStageThreeFourDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : STAGE_THREE_FOUR_DOCS) {
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
