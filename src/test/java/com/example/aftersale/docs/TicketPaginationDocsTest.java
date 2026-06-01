package com.example.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class TicketPaginationDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String COMPLETED_PLAN =
            "docs/exec-plans/completed/"
                    + "EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE3_2_TICKET_PAGINATION.md";

    private static final List<String> STAGE_THREE_TWO_DOCS = List.of(
            "README.md",
            "docs/api/OPENAPI.md",
            "docs/decisions/DECISION_PROJECT_REVIEW_API_COMPLETENESS.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/exec-plans/active/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            COMPLETED_PLAN);

    @Test
    void ticketPaginationCompletionRecordExistsAndIsLinked() throws IOException {
        String readme = projectText("README.md");
        String completion = projectText(COMPLETED_PLAN);
        String activePlan = projectText("docs/exec-plans/active/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md");

        assertThat(completion).contains(
                "Status: Completed",
                "Ticket Pagination Boundary",
                "Query Filter Boundary",
                "API Compatibility Boundary",
                "OpenAPI Documentation Boundary",
                "ToolRegistry / Agent Boundary",
                "Default Offline Boundary",
                "TASK_COMPLETE");
        assertThat(readme).contains(COMPLETED_PLAN);
        assertThat(activePlan).contains(
                "状态：阶段 0-3.4 已完成",
                "阶段 3.2：Ticket list/query pagination foundation",
                "状态：已完成",
                COMPLETED_PLAN);
    }

    @Test
    void docsRecordTicketListEndpointParametersAndFilters() throws IOException {
        String docs = combinedStageThreeTwoDocs();

        assertThat(docs).contains(
                "Project Review Correction Stage 3.2 (completed)",
                "Ticket Pagination Foundation Validation",
                "Ticket list/query pagination foundation",
                "`GET /api/tickets`",
                "GET /api/tickets?page=0&size=20&sort=createdAt,desc",
                "`page`",
                "`size`",
                "`sort`",
                "`status`",
                "`userId`",
                "`orderId`",
                "`intentType`",
                "`createdFrom`",
                "`createdTo`",
                "createdAt",
                "updatedAt",
                "ticketId");
    }

    @Test
    void docsPreserveAgentToolRegistryAndRagBoundaries() throws IOException {
        String docs = combinedStageThreeTwoDocs();

        assertThat(docs).contains(
                "does not create AgentRun",
                "不创建 AgentRun",
                "不调用 ToolRegistry",
                "不暴露 public RAG HTTP endpoint",
                "`search_aftersale_policy`",
                "LOW-risk read-only ToolRegistry tool",
                "RAG evidence",
                "policy evidence");
        assertThat(docs).contains(
                "does not write",
                "`ToolCallTrace`",
                "does not write Workspace",
                "does not invoke RAG retrieval");
    }

    @Test
    void laterApiWorkRemainsFuture() throws IOException {
        String docs = combinedStageThreeTwoDocs();
        String lower = docs.toLowerCase(Locale.ROOT);

        assertThat(docs).contains(
                "async AgentRun",
                "SSE / WebSocket",
                "batch API",
                "production auth / RBAC",
                "planned",
                "future",
                "Stage 3.4");
        assertThat(docs).contains(
                "AgentRun get/status polling",
                "Project Review Correction Stage 3.3 (completed)");
        assertThat(lower).doesNotContain(
                "production-grade async agentrun completed",
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
        for (String path : STAGE_THREE_TWO_DOCS) {
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

    private static String combinedStageThreeTwoDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : STAGE_THREE_TWO_DOCS) {
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
