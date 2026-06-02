package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class RagQualityDecisionDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String DECISION_DOC =
            "docs/decisions/DECISION_PROJECT_REVIEW_RAG_QUALITY_IMPROVEMENT.md";

    private static final String COMPLETED_PLAN =
            "version-updates/"
                    + "EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE5_RAG_QUALITY_EVALUATION.md";

    private static final List<String> STAGE_FIVE_DOCS = List.of(
            "README.md",
            "docs/agent/RAG_POLICY_RETRIEVAL_CONTRACT.md",
            "docs/decisions/DECISION_V4_RAG_VECTOR_STORE.md",
            "docs/evaluation/EVALUATION.md",
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
        String ragContract = projectText("docs/agent/RAG_POLICY_RETRIEVAL_CONTRACT.md");
        String vectorDecision = projectText("docs/decisions/DECISION_V4_RAG_VECTOR_STORE.md");
        String evaluation = projectText("docs/evaluation/EVALUATION.md");
        String remediation = projectText("docs/quality/PROJECT_REMEDIATION_PLAN.md");
        String validation = projectText("docs/quality/VALIDATION_COMMANDS.md");
        String quality = projectText("docs/quality/QUALITY_SCORE.md");
        String activePlan = projectText("version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md");

        assertThat(decision).contains(
                "Status: Completed",
                "Current RAG Baseline",
                "Current Evaluation Baseline",
                "Reranking Evaluation",
                "Query Rewriting Evaluation",
                "RRF / Hybrid Scoring Evaluation",
                "Chunk Window Expansion Evaluation",
                "Provider / PGvector Boundary",
                "ToolRegistry / Evidence-only Boundary",
                "TASK_COMPLETE");
        assertThat(completion).contains("Status: Completed", "TASK_COMPLETE");
        assertThat(readme).contains(DECISION_DOC, COMPLETED_PLAN);
        assertThat(ragContract).contains(DECISION_DOC, "Project Review Stage 5 RAG Quality Evaluation");
        assertThat(vectorDecision).contains(DECISION_DOC, "Stage 5 quality evaluation status");
        assertThat(evaluation).contains("RAG Retrieval Quality Improvement Roadmap", DECISION_DOC);
        assertThat(remediation).contains("阶段 5：planned", "阶段 5：已完成", DECISION_DOC);
        assertThat(validation).contains("RagQualityDecisionDocsTest", DECISION_DOC);
        assertThat(quality).contains("Project Review Correction Stage 5 (completed)", DECISION_DOC);
        assertThat(activePlan).contains("状态：阶段 0-4 已完成", "状态：阶段 0-5 已完成", DECISION_DOC, COMPLETED_PLAN);
    }

    @Test
    void currentRagAndEvaluationBaselinesAreDocumented() throws IOException {
        String docs = combinedStageFiveDocs();

        assertThat(docs).contains(
                "KEYWORD / VECTOR / HYBRID",
                "RagPolicyEvidenceMergeService",
                "EmbeddingClient abstraction",
                "PolicyVectorRepository contract",
                "FakeEmbeddingClient",
                "InMemoryPolicyVectorRepository",
                "deterministic RAG evaluation",
                "no LLM-as-judge by default");
    }

    @Test
    void futureRagQualityCapabilitiesRemainNotImplemented() throws IOException {
        String docs = combinedStageFiveDocs();
        String lower = docs.toLowerCase(Locale.ROOT);

        assertThat(docs).contains(
                "reranking is not implemented",
                "query rewriting is not implemented",
                "RRF is not implemented",
                "chunk window expansion is not implemented",
                "JdbcPolicyVectorRepository is not implemented",
                "live PGvector validation is not completed",
                "Spring AI VectorStore production path is not enabled",
                "real reranker/embedding provider must be opt-in");
        assertThat(lower).doesNotContain(
                "reranking runtime completed",
                "query rewriting runtime completed",
                "rrf runtime completed",
                "chunk window expansion runtime completed",
                "hybrid scoring runtime changes completed",
                "jdbcpolicyvectorrepository completed",
                "live pgvector validation completed",
                "spring ai vectorstore production path completed",
                "public rag http endpoint completed");
    }

    @Test
    void evidenceToolApprovalAndTraceBoundariesAreDocumented() throws IOException {
        String docs = combinedStageFiveDocs();

        assertThat(docs).contains(
                "search_aftersale_policy remains LOW-risk read-only ToolRegistry tool",
                "RAG evidence is evidence-only",
                "RAG score is not business decision confidence",
                "high-risk actions require Approval",
                "LLM must not directly execute tools",
                "future RAG improvements must not bypass ToolRegistry / RiskPolicy / Approval / Trace / Workspace / "
                        + "Execution Tree");
    }

    @Test
    void runtimeNonChangeAndDefaultOfflineBoundariesAreDocumented() throws IOException {
        String docs = combinedStageFiveDocs();

        assertThat(docs).contains(
                "Stage 5 changes docs and docs harness tests only",
                "does not modify `src/main/java`",
                "does not modify `search_aftersale_policy`",
                "retrieval algorithms",
                "RAG evaluation runner",
                "real LLM",
                "API Key",
                "PostgreSQL",
                "PGvector",
                "Docker",
                "MySQL",
                "Redis",
                "real embedding provider",
                "real reranker provider",
                "Spring AI VectorStore",
                "external network");
    }

    @Test
    void docsDoNotContainSecretsLocalPathsOrRuntimeOverclaims() throws IOException {
        for (String path : STAGE_FIVE_DOCS) {
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
                "reranking runtime completed",
                "query rewriting runtime completed",
                "rrf runtime completed",
                "chunk window expansion runtime completed",
                "jdbcpolicyvectorrepository completed",
                "live pgvector validation completed",
                "spring ai vectorstore production path completed",
                "production deployment completed",
                "production monitoring completed",
                "production auth completed",
                "真实退款已接入",
                "真实换货已接入",
                "真实补偿已接入",
                "真实支付已接入",
                "真实物流已接入");
    }

    private static String combinedStageFiveDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : STAGE_FIVE_DOCS) {
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
