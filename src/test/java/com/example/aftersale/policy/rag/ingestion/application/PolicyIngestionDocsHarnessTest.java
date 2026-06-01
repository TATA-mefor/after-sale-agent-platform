package com.example.aftersale.policy.rag.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PolicyIngestionDocsHarnessTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    @Test
    void ingestionPipelineDocsAndCompletionRecordExist() throws IOException {
        String pipelineDoc = projectText("docs/demo/V4_POLICY_INGESTION_PIPELINE.md");
        String completionRecord = projectText(
                "docs/exec-plans/completed/EXEC_PLAN_V4_POLICY_INGESTION_FOUNDATION.md");

        assertThat(pipelineDoc).contains("V4.4 provides the policy ingestion pipeline foundation");
        assertThat(pipelineDoc).contains("admin / offline pipeline");
        assertThat(pipelineDoc).contains("capability, not an Agent automatic tool");
        assertThat(pipelineDoc).contains("FakeEmbeddingClient");
        assertThat(pipelineDoc).contains("InMemoryPolicyIngestionRepository");
        assertThat(pipelineDoc).contains("InMemoryPolicyVectorRepository");
        assertThat(completionRecord).contains("Status: Completed");
        assertThat(completionRecord).contains("V4.4.1 Summary");
        assertThat(completionRecord).contains("V4.4.2 Summary");
        assertThat(completionRecord).contains("V4.4.3 Summary");
        assertThat(completionRecord).contains("V4.4.4 Summary");
        assertThat(completionRecord).contains("TASK_COMPLETE");
    }

    @Test
    void ingestionDocsDescribeBoundariesAndNonGoals() throws IOException {
        String docs = projectText("README.md")
                + "\n"
                + projectText("docs/demo/V4_POLICY_INGESTION_PIPELINE.md")
                + "\n"
                + projectText("docs/agent/RAG_POLICY_RETRIEVAL_CONTRACT.md")
                + "\n"
                + projectText("docs/decisions/DECISION_V4_RAG_VECTOR_STORE.md")
                + "\n"
                + projectText("docs/quality/QUALITY_SCORE.md")
                + "\n"
                + projectText("docs/exec-plans/completed/EXEC_PLAN_V4_POLICY_INGESTION_FOUNDATION.md");

        assertThat(docs).contains("V4.4 is an ingestion foundation");
        assertThat(docs).contains("V4.4 Policy Ingestion Foundation Quality Summary");
        assertThat(docs).contains("V4.4 Ingestion Pipeline Foundation");
        assertThat(docs).contains("Policy ingestion does not enter ToolRegistry or Agent runtime in V4.4");
        assertThat(docs).contains("no Admin Controller yet");
        assertThat(docs).contains("no `ingest_policy_document` tool");
        assertThat(docs).contains("no ToolRegistry wiring");
        assertThat(docs).contains("no real Spring AI embedding default path");
        assertThat(docs).contains("no `JdbcPolicyVectorRepository`");
        assertThat(docs).contains("no PGvector live writes");
        assertThat(docs).contains("no RAG / HYBRID retrieval in the V4.4 ingestion stage itself");
        assertThat(docs).contains("no Agent runtime vector retrieval in V4.4 itself");
        assertThat(docs)
                .contains("`search_aftersale_policy` HYBRID retrieval through ToolRegistry using the default fake"
                        + " / in-memory vector path");
        assertThat(docs).contains("V4.5 wires HYBRID RAG into `search_aftersale_policy`");
    }

    @Test
    void ingestionDocsDescribeDefaultOfflineIsolation() throws IOException {
        String docs = projectText("docs/demo/V4_POLICY_INGESTION_PIPELINE.md")
                + "\n"
                + projectText("docs/quality/QUALITY_SCORE.md")
                + "\n"
                + projectText("docs/exec-plans/completed/EXEC_PLAN_V4_POLICY_INGESTION_FOUNDATION.md");

        assertThat(docs).contains("does not require an API key");
        assertThat(docs).contains("PostgreSQL");
        assertThat(docs).contains("PGvector");
        assertThat(docs).contains("Docker");
        assertThat(docs).contains("MySQL");
        assertThat(docs).contains("Redis");
        assertThat(docs).contains("real LLM");
        assertThat(docs).contains("external network");
    }

    @Test
    void ingestionDocsDoNotContainRealSecretsOrLocalPaths() throws IOException {
        assertSecretSafe(projectText("docs/demo/V4_POLICY_INGESTION_PIPELINE.md"));
        assertSecretSafe(projectText("docs/exec-plans/completed/EXEC_PLAN_V4_POLICY_INGESTION_FOUNDATION.md"));
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
        assertThat(lower).doesNotContain("jdbc:postgresql://prod");
        assertThat(lower).doesNotContain("jdbc:postgresql://production");
        assertThat(lower).doesNotContain("d:\\");
        assertThat(lower).doesNotContain("c:\\");
        assertThat(lower).doesNotContain("/users/");
        assertThat(lower).doesNotContain("data/raw/");
    }
}
