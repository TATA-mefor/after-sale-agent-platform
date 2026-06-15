package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectReviewCorrectionDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final List<String> REVIEW_CORRECTION_DOCS = List.of(
            "README.md",
            "version-updates/EXEC_PLAN_V4.md",
            "version-updates/EXEC_PLAN_V4_RAG_SPRING_AI.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE0.md",
            "version-updates/V4_RELEASE_SUMMARY.md",
            "version-updates/V4_FACTS.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "docs/demo/V4_PGVECTOR_LOCAL_SETUP.md",
            "docs/demo/V4_POLICY_INGESTION_PIPELINE.md",
            "docs/demo/V4_PROJECT_HIGHLIGHTS.md",
            "docs/demo/V4_INTERVIEW_DEMO_CHECKLIST.md",
            "docs/api/OPENAPI.md");

    @Test
    void correctionCompletionRecordExistsAndIsLinked() throws IOException {
        String readme = projectText("README.md");
        String execPlan = projectText("version-updates/EXEC_PLAN_V4.md");
        String release = projectText("version-updates/V4_RELEASE_SUMMARY.md");
        String completion = projectText(
                "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE0.md");

        assertThat(readme).contains("Project Remediation Plan", "PROJECT_REMEDIATION_PLAN.md");
        assertThat(execPlan).contains("version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE0.md");
        assertThat(release).contains("version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE0.md");
        assertThat(completion).contains(
                "Status: Completed",
                "Documentation Fact Correction Boundary",
                "PGvector / Vector Store Wording Boundary",
                "API Surface Wording Boundary",
                "Production Hardening Boundary",
                "Runtime Non-change Boundary",
                "Default Offline Validation Boundary",
                "TASK_COMPLETE");
    }

    @Test
    void docsDistinguishV4FoundationCompletionFromProductionHardening() throws IOException {
        String docs = combinedReviewDocs();

        assertThat(docs).contains(
                "foundation / demo / interview-grade",
                "production deployment",
                "production auth",
                "production monitoring",
                "V5 production hardening",
                "not a production deployment guide");
        assertThat(docs).contains("不表示 production deployment");
    }

    @Test
    void pgvectorAndVectorStoreWordingStaysFoundationAndOptIn() throws IOException {
        String docs = combinedReviewDocs();

        assertThat(docs).contains(
                "PGvector 当前是 profile、schema、compose、docs、repository contract",
                "fake / in-memory",
                "V5.A.1",
                "JdbcPolicyVectorRepository",
                "opt-in profile",
                "live PGvector validation",
                "Default live PGvector write/search",
                "Spring AI `VectorStore` production path");
        assertThat(docs).doesNotContain(
                "JdbcPolicyVectorRepository implementation completed",
                "default live PGvector persistence completed",
                "Spring AI VectorStore production path completed",
                "live PGvector validation completed");
    }

    @Test
    void apiSpringAiRagAndObservabilityGapsAreFutureWork() throws IOException {
        String docs = combinedReviewDocs();

        assertThat(docs).contains(
                "demo/backend API surface",
                "不是完整生产 CRUD 平台",
                "ChatMemory",
                "Advisors",
                "Tool Calling API",
                "bulk embedding",
                "reranking",
                "query rewriting",
                "RRF",
                "chunk window expansion",
                "Prometheus registry",
                "metrics dashboard",
                "distributed tracing",
                "cross-service trace-id propagation");
        assertThat(docs).doesNotContain(
                "complete CRUD API completed",
                "Prometheus registry completed",
                "distributed tracing completed",
                "ChatMemory completed",
                "Tool Calling API completed",
                "reranking completed",
                "query rewriting completed",
                "RRF completed");
    }

    @Test
    void docsKeepRagToolRegistryIngestionAndOfflineBoundaries() throws IOException {
        String docs = combinedReviewDocs();

        assertThat(docs).contains(
                "RAG evidence",
                "policy evidence",
                "LOW-risk read-only ToolRegistry tool",
                "ToolRegistry 仍是 Agent tool execution",
                "Skill 不替代 ToolRegistry",
                "admin/offline pipeline",
                "not an Agent runtime tool",
                "default offline",
                "does not require API keys");
    }

    @Test
    void correctionDocsDoNotContainSecretsLocalPathsOrProductionOverclaims() throws IOException {
        for (String path : REVIEW_CORRECTION_DOCS) {
            String lower = projectText(path).toLowerCase();

            assertThat(lower).as(path).doesNotContain(
                    "d:/",
                    "d:\\",
                    "c:/",
                    "c:\\",
                    "/users/",
                    "openai_api_key=",
                    "dashscope_api_key=",
                    "spring_ai_openai_api_key=",
                    "token=",
                    "secret=",
                    "sk-",
                    "real refund integration completed",
                    "real exchange integration completed",
                    "real payment integration completed",
                    "real logistics integration completed",
                    "production deployment completed",
                    "production monitoring completed",
                    "production auth completed");
        }
    }

    private static String combinedReviewDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : REVIEW_CORRECTION_DOCS) {
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
