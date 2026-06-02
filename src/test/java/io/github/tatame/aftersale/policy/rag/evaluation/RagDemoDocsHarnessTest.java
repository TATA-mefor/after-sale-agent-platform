package io.github.tatame.aftersale.policy.rag.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RagDemoDocsHarnessTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    @Test
    void ragDemoScriptDocumentsV462ScenariosAndBoundaries() throws IOException {
        String demo = projectText("docs/demo/V4_RAG_DEMO_SCRIPT.md");

        assertThat(demo).contains("V4 RAG Demo Script");
        assertThat(demo).contains("Scenario A: HYBRID Policy Search Tool Demo");
        assertThat(demo).contains("Scenario B: AgentRun With RAG Evidence");
        assertThat(demo).contains("Scenario C: Execution Tree Evidence View");
        assertThat(demo).contains("Scenario D: RAG Evaluation");
        assertThat(demo).contains("search_aftersale_policy");
        assertThat(demo).contains("LOW-risk read-only");
        assertThat(demo).contains("evidence-only");
        assertThat(demo).contains("default demo path is offline");
        assertThat(demo).contains("does not require an API key, PostgreSQL,\nPGvector, Docker");
        assertThat(demo).contains("Optional Live Paths");
        assertThat(demo).contains("Expected output shape");
        assertThat(demo).contains("\"retrievalMode\": \"HYBRID\"");
        assertThat(demo).contains("\"evidences\"");
        assertThat(demo).contains("\"fallbackUsed\": false");
        assertThat(demo).contains("V4.6.2 adds this demo script and docs harness coverage only");
        assertThat(demo).contains("does not add runtime behavior");
    }

    @Test
    void ragDemoDocsAreLinkedFromReadmeAndEvaluationDocs() throws IOException {
        String readmeAndInterview = projectText("README.md")
                + "\n"
                + projectText("docs/demo/DEMO_INTERVIEW_GUIDE.md");
        String v4Roadmap = projectText("version-updates/V4_ROADMAP.md");
        String evaluationDocs = projectText("docs/evaluation/EVALUATION.md");

        assertThat(readmeAndInterview).contains("docs/demo/V4_RAG_DEMO_SCRIPT.md");
        assertThat(readmeAndInterview).contains("docs/demo/V4_POLICY_INGESTION_PIPELINE.md");
        assertThat(readmeAndInterview).contains("docs/demo/V4_PGVECTOR_LOCAL_SETUP.md");
        assertThat(readmeAndInterview).contains("docs/evaluation/EVALUATION.md");
        assertThat(v4Roadmap).contains("default V4 RAG demo does not require API keys, Docker, or PGvector");
        assertThat(v4Roadmap).contains("local interview / project review demo");

        assertThat(evaluationDocs).contains("Scenario D");
        assertThat(evaluationDocs).contains("docs/demo/V4_RAG_DEMO_SCRIPT.md");
        assertThat(evaluationDocs).contains("mvn test -Dtest=RagEvaluationApplicationServiceTest");
        assertThat(evaluationDocs).contains("does not use LLM-as-judge");
        assertThat(evaluationDocs).contains("does not call a real embedding provider");
    }

    @Test
    void ragDemoPlanAndRoadmapDescribeV462Completion() throws IOException {
        String docs = projectText("version-updates/EXEC_PLAN_V4_RAG_DEMO_SCRIPT.md")
                + "\n"
                + projectText("version-updates/EXEC_PLAN_V4.md")
                + "\n"
                + projectText("version-updates/EXEC_PLAN_V4_RAG_SPRING_AI.md")
                + "\n"
                + projectText("docs/quality/QUALITY_SCORE.md");

        assertThat(docs).contains("V4.6.2");
        assertThat(docs).contains("Status: Completed");
        assertThat(docs).contains("Demo Script Boundary");
        assertThat(docs).contains("Offline Demo Boundary");
        assertThat(docs).contains("Evidence-only Demo Boundary");
        assertThat(docs).contains("Evaluation Demo Boundary");
        assertThat(docs).contains("Default Test Boundary");
        assertThat(docs).contains("demo script / expected output / docs harness");
        assertThat(docs).contains("does not add runtime behavior");
        assertThat(docs).contains("V4.6.3");
        assertThat(docs).contains("Actuator health indicators");
        assertThat(docs).contains("V4.6.4");
        assertThat(docs).contains("OpenAPI");
        assertThat(docs).contains("TASK_COMPLETE");
    }

    @Test
    void ragDemoDocsDoNotContainSecretsLocalPathsOrCompletedActionClaims() throws IOException {
        assertSecretSafe(projectText("docs/demo/V4_RAG_DEMO_SCRIPT.md"));
        assertSecretSafe(projectText("version-updates/EXEC_PLAN_V4_RAG_DEMO_SCRIPT.md"));
    }

    private static String projectText(String path) throws IOException {
        Path file = PROJECT_ROOT.resolve(path);
        assertThat(Files.exists(file)).as(path + " should exist").isTrue();
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private static void assertSecretSafe(String text) {
        String lower = text.toLowerCase();

        assertThat(lower).doesNotContain("sk-");
        assertThat(lower).doesNotContain("api_key=");
        assertThat(lower).doesNotContain("password=prod");
        assertThat(lower).doesNotContain("jdbc:postgresql://prod");
        assertThat(lower).doesNotContain("jdbc:postgresql://production");
        assertThat(lower).doesNotContain("d:\\");
        assertThat(lower).doesNotContain("c:\\");
        assertThat(lower).doesNotContain("/users/");
        assertThat(lower).doesNotContain("data/raw/");
        assertThat(lower).doesNotContain("完整 prompt");
        assertThat(lower).doesNotContain("full prompt");
        assertThat(lower).doesNotContain("已退款成功");
        assertThat(lower).doesNotContain("已换货完成");
        assertThat(lower).doesNotContain("已补偿到账");
    }
}
