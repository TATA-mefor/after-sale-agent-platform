package com.example.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiCompletenessDecisionDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String DECISION_DOC =
            "docs/decisions/DECISION_PROJECT_REVIEW_API_COMPLETENESS.md";

    private static final String COMPLETED_PLAN =
            "docs/exec-plans/completed/"
                    + "EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE3_1_API_COMPLETENESS_DECISION.md";

    private static final List<String> STAGE_THREE_ONE_DOCS = List.of(
            "README.md",
            "docs/api/OPENAPI.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/exec-plans/active/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            DECISION_DOC,
            COMPLETED_PLAN);

    @Test
    void apiCompletenessDecisionAndCompletionRecordExistAndAreLinked() throws IOException {
        String readme = projectText("README.md");
        String openApiDocs = projectText("docs/api/OPENAPI.md");
        String validation = projectText("docs/quality/VALIDATION_COMMANDS.md");
        String remediation = projectText("docs/quality/PROJECT_REMEDIATION_PLAN.md");
        String quality = projectText("docs/quality/QUALITY_SCORE.md");
        String activePlan = projectText("docs/exec-plans/active/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md");
        String completedPlan = projectText(COMPLETED_PLAN);

        assertThat(projectText(DECISION_DOC)).contains("Status: Completed");
        assertThat(completedPlan).contains("Status: Completed", "TASK_COMPLETE");
        assertThat(readme).contains(DECISION_DOC, COMPLETED_PLAN);
        assertThat(openApiDocs).contains("API Completeness Roadmap", DECISION_DOC);
        assertThat(validation).contains("ApiCompletenessDecisionDocsTest", DECISION_DOC);
        assertThat(remediation).contains("阶段 3.1：已完成", DECISION_DOC);
        assertThat(quality).contains("Project Review Correction Stage 3.1 (completed)", DECISION_DOC);
        assertThat(activePlan).contains("状态：阶段 0-3.3 已完成", DECISION_DOC, COMPLETED_PLAN);
    }

    @Test
    void decisionRecordsCurrentHttpApiSurface() throws IOException {
        String decision = projectText(DECISION_DOC);

        assertThat(decision).contains(
                "demo/backend API surface",
                "不是完整生产 CRUD",
                "Ticket",
                "`POST /api/tickets`",
                "`GET /api/tickets`",
                "`GET /api/tickets/{ticketId}`",
                "AgentRun",
                "`POST /api/tickets/{ticketId}/agent-runs`",
                "`GET /api/agent-runs/{runId}`",
                "ToolCallTrace 只读审计视图",
                "Execution Tree",
                "`GET /api/agent-runs/{runId}/execution-tree`",
                "`GET /api/approval-requests/pending`",
                "`GET /api/approval-requests/{approvalRequestId}`",
                "`POST /api/approval-requests/{approvalRequestId}/approve`",
                "`POST /api/approval-requests/{approvalRequestId}/reject`",
                "`GET /actuator/health`",
                "`/v3/api-docs`",
                "Swagger UI");
    }

    @Test
    void futureApiWorkIsPlannedNotStageThreeOneRuntime() throws IOException {
        String docs = combinedStageThreeOneDocs();
        String lower = docs.toLowerCase();

        assertThat(docs).contains(
                "Ticket list/query pagination",
                "production-grade async AgentRun",
                "SSE / WebSocket",
                "batch API",
                "production auth / RBAC",
                "future work",
                "planned",
                "阶段 3.1",
                "阶段 3.2",
                "阶段 3.3",
                "阶段 3.4");
        assertThat(docs).contains(
                "AgentRun get/status polling",
                "已完成",
                "Project Review Correction Stage 3.3");
        assertThat(lower).doesNotContain(
                "complete crud api completed",
                "production api hardening completed",
                "async agentrun completed",
                "sse completed",
                "websocket completed",
                "batch api completed",
                "production auth completed",
                "rbac completed");
    }

    @Test
    void toolRegistryApprovalAndRagHttpBoundariesArePreserved() throws IOException {
        String docs = combinedStageThreeOneDocs();

        assertThat(docs).contains(
                "`search_aftersale_policy`",
                "LOW-risk read-only ToolRegistry tool",
                "不是 public RAG HTTP endpoint",
                "ToolRegistry 仍是 Agent tool execution entry",
                "LLM 可以规划工具，但不得直接执行工具",
                "RiskPolicy / Approval",
                "高风险动作",
                "RAG evidence",
                "policy evidence");
        assertThat(docs).contains(
                "不新增 public RAG HTTP endpoint",
                "不接入真实退款、换货、补偿、支付或物流系统");
    }

    @Test
    void runtimeNonChangeAndDefaultOfflineBoundariesAreDocumented() throws IOException {
        String docs = combinedStageThreeOneDocs();

        assertThat(docs).contains(
                "不新增 endpoint",
                "不修改 Controller",
                "OpenAPI config",
                "Runtime non-change",
                "只读文档",
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
                "external network");
    }

    @Test
    void docsDoNotContainSecretsLocalPathsOrApiOverclaims() throws IOException {
        for (String path : STAGE_THREE_ONE_DOCS) {
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
                "sk-",
                "complete crud api completed",
                "production api hardening completed",
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

    private static String combinedStageThreeOneDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : STAGE_THREE_ONE_DOCS) {
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
