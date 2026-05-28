package com.example.aftersale.policy.rag.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RagEvaluationDocsHarnessTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    @Test
    void ragEvaluationDocsDescribeV461Boundary() throws IOException {
        String docs = projectText("docs/evaluation/EVALUATION.md")
                + "\n"
                + projectText("EXEC_PLAN_V4.md")
                + "\n"
                + projectText("docs/exec-plans/active/EXEC_PLAN_V4_RAG_SPRING_AI.md")
                + "\n"
                + projectText("docs/agent/RAG_POLICY_RETRIEVAL_CONTRACT.md")
                + "\n"
                + projectText("docs/quality/QUALITY_SCORE.md")
                + "\n"
                + projectText("README.md")
                + "\n"
                + projectText("docs/exec-plans/completed/EXEC_PLAN_V4_RAG_EVALUATION_CASES_METRICS.md");

        assertThat(docs).contains("V4.6.1");
        assertThat(docs).contains("docs/evaluation/rag_policy_cases.jsonl");
        assertThat(docs).contains("FakeEmbeddingClient");
        assertThat(docs).contains("InMemoryPolicyVectorRepository");
        assertThat(docs).contains("evidenceRecallPassRate");
        assertThat(docs).contains("evidenceSourcePassRate");
        assertThat(docs).contains("retrievalModePassRate");
        assertThat(docs).contains("fallbackAccuracy");
        assertThat(docs).contains("emptyResultAccuracy");
        assertThat(docs).contains("citationCompletenessRate");
        assertThat(docs).contains("safetyPassRate");
        assertThat(docs).contains("does not use LLM-as-judge");
        assertThat(docs).contains("does not call real LLMs");
        assertThat(docs).contains("does not connect PostgreSQL / PGvector");
        assertThat(docs).contains("V2.9 evaluation");
        assertThat(docs).contains("V4.6.1 evaluates policy evidence retrieval");
        assertThat(docs).contains("V4.6.2");
        assertThat(docs).contains("V4.6.3");
        assertThat(docs).contains("V4.6.4");
    }

    @Test
    void ragEvaluationCompletionRecordExists() throws IOException {
        String completionRecord =
                projectText("docs/exec-plans/completed/EXEC_PLAN_V4_RAG_EVALUATION_CASES_METRICS.md");

        assertThat(completionRecord).contains("Status: Completed");
        assertThat(completionRecord).contains("Dataset Boundary");
        assertThat(completionRecord).contains("Metrics Boundary");
        assertThat(completionRecord).contains("Runner Boundary");
        assertThat(completionRecord).contains("Default Offline Test Boundary");
        assertThat(completionRecord).contains("Architecture Boundary");
        assertThat(completionRecord).contains("TASK_COMPLETE");
    }

    @Test
    void ragEvaluationDocsDoNotContainRealSecretsOrLocalPaths() throws IOException {
        assertSecretSafe(projectText("docs/evaluation/rag_policy_cases.jsonl"));
        assertSecretSafe(projectText("docs/evaluation/EVALUATION.md"));
        assertSecretSafe(projectText("docs/exec-plans/completed/EXEC_PLAN_V4_RAG_EVALUATION_CASES_METRICS.md"));
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
    }
}
