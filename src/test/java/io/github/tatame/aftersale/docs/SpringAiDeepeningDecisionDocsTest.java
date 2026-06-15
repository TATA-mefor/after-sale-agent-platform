package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class SpringAiDeepeningDecisionDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String DECISION_DOC =
            "docs/decisions/DECISION_PROJECT_REVIEW_SPRING_AI_DEEPENING.md";

    private static final String COMPLETED_PLAN =
            "version-updates/"
                    + "EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE4_SPRING_AI_DEEPENING_EVALUATION.md";

    private static final List<String> STAGE_FOUR_DOCS = List.of(
            "README.md",
            "docs/decisions/DECISION_V4_SPRING_AI_ADAPTER.md",
            DECISION_DOC,
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "docs/quality/QUALITY_SCORE.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            COMPLETED_PLAN);

    @Test
    void decisionAndCompletionRecordExistAndAreLinked() throws IOException {
        String decision = projectText(DECISION_DOC);
        String completion = projectText(COMPLETED_PLAN);
        String readme = projectText("README.md");
        String v4SpringAiDecision = projectText("docs/decisions/DECISION_V4_SPRING_AI_ADAPTER.md");
        String remediation = projectText("docs/quality/PROJECT_REMEDIATION_PLAN.md");
        String validation = projectText("docs/quality/VALIDATION_COMMANDS.md");
        String quality = projectText("docs/quality/QUALITY_SCORE.md");
        String activePlan = projectText("version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md");

        assertThat(decision).contains(
                "Status: Completed",
                "Current Spring AI Baseline",
                "ChatMemory Evaluation",
                "Advisor Evaluation",
                "Tool Calling API Evaluation",
                "Bulk Embedding Evaluation",
                "Provider Governance Relationship",
                "ToolRegistry / Approval Boundary",
                "TASK_COMPLETE");
        assertThat(completion).contains("Status: Completed", "TASK_COMPLETE");
        assertThat(readme).contains("Spring AI", "SPEC.md");
        assertThat(v4SpringAiDecision).contains(DECISION_DOC, "Project Review Stage 4 Deepening Evaluation");
        assertThat(remediation).contains("阶段 4：已完成", DECISION_DOC, "阶段 5：planned");
        assertThat(validation).contains("SpringAiDeepeningDecisionDocsTest", DECISION_DOC);
        assertThat(quality).contains("Project Review Correction Stage 4 (completed)", DECISION_DOC);
        assertThat(activePlan).contains("状态：阶段 0-4 已完成", DECISION_DOC, COMPLETED_PLAN);
    }

    @Test
    void springAiBaselineIsDocumented() throws IOException {
        String docs = combinedStageFourDocs();

        assertThat(docs).contains(
                "Spring AI Chat adapter foundation",
                "Spring AI embedding adapter foundation",
                "LlmClient abstraction",
                "EmbeddingClient abstraction",
                "FakeEmbeddingClient",
                "live Spring AI smoke tests are opt-in");
    }

    @Test
    void deepeningCapabilitiesRemainEvaluationOnly() throws IOException {
        String docs = combinedStageFourDocs();
        String lower = docs.toLowerCase(Locale.ROOT);

        assertThat(docs).contains(
                "ChatMemory is not implemented",
                "Advisors are not implemented",
                "Spring AI Tool Calling API is not enabled",
                "bulk embedding runtime is not implemented",
                "Stage 4 completed the decision/evaluation only");
        assertThat(lower).doesNotContain(
                "chatmemory runtime completed",
                "advisors runtime completed",
                "advisor runtime completed",
                "spring ai tool calling api completed",
                "spring ai tool calling api enabled by default",
                "bulk embedding runtime completed",
                "bulk embedding completed",
                "provider runtime changes completed");
    }

    @Test
    void toolRegistryAgentApprovalAndBulkEmbeddingBoundariesAreDocumented() throws IOException {
        String docs = combinedStageFourDocs();

        assertThat(docs).contains(
                "Spring AI Tool Calling API cannot replace ToolRegistry",
                "LLM must not directly execute tools",
                "ToolRegistry boundary must be preserved",
                "AgentPlanParser",
                "AgentPlanValidator",
                "must not be bypassed",
                "high-risk actions still require Approval",
                "bulk embedding must stay behind EmbeddingClient abstraction",
                "RAG evidence",
                "policy evidence");
    }

    @Test
    void runtimeNonChangeAndDefaultOfflineBoundariesAreDocumented() throws IOException {
        String docs = combinedStageFourDocs();

        assertThat(docs).contains(
                "Stage 4 changes docs and docs harness tests only",
                "does not modify `src/main/java`",
                "src/main/resources",
                "pom.xml",
                "real LLM",
                "API Key",
                "PostgreSQL",
                "PGvector",
                "Docker",
                "MySQL",
                "Redis",
                "real embedding provider",
                "Spring AI live provider calls",
                "Spring AI VectorStore",
                "external network");
    }

    @Test
    void docsDoNotContainSecretsLocalPathsOrSpringAiRuntimeOverclaims() throws IOException {
        for (String path : STAGE_FOUR_DOCS) {
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
                "chatmemory runtime completed",
                "advisor runtime completed",
                "advisors runtime completed",
                "spring ai tool calling api completed",
                "spring ai tool calling api enabled by default",
                "bulk embedding runtime completed",
                "spring ai deepening runtime completed",
                "production deployment completed",
                "production monitoring completed",
                "production auth completed",
                "真实退款已接入",
                "真实换货已接入",
                "真实支付已接入",
                "真实物流已接入");
    }

    private static String combinedStageFourDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : STAGE_FOUR_DOCS) {
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
