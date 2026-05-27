package com.example.aftersale.policy.rag.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RagSearchDocsHarnessTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    @Test
    void ragSearchContractDocsDescribeV451Boundary() throws IOException {
        String docs = projectText("docs/agent/TOOL_CONTRACTS.md")
                + "\n"
                + projectText("docs/agent/RAG_POLICY_RETRIEVAL_CONTRACT.md")
                + "\n"
                + projectText("README.md")
                + "\n"
                + projectText("docs/quality/QUALITY_SCORE.md")
                + "\n"
                + projectText("docs/exec-plans/completed/EXEC_PLAN_V4_RAG_SEARCH_CONTRACT.md");

        assertThat(docs).contains("retrievalMode");
        assertThat(docs).contains("V4.5.1");
        assertThat(docs).contains("schema preparation only");
        assertThat(docs).contains("does not change `search_aftersale_policy` runtime");
        assertThat(docs).contains("LOW-risk read-only");
        assertThat(docs).contains("evidence only");
        assertThat(docs).contains("V4.5.2");
        assertThat(docs).contains("V4.5.3");
        assertThat(docs).contains("V4.5.4");
    }

    @Test
    void ragSearchCompletionRecordExists() throws IOException {
        String completionRecord = projectText("docs/exec-plans/completed/EXEC_PLAN_V4_RAG_SEARCH_CONTRACT.md");

        assertThat(completionRecord).contains("Status: Completed");
        assertThat(completionRecord).contains("Retrieval Mode Boundary");
        assertThat(completionRecord).contains("RAG Evidence Boundary");
        assertThat(completionRecord).contains("Mapper Boundary");
        assertThat(completionRecord).contains("TASK_COMPLETE");
    }

    @Test
    void ragSearchDocsDoNotContainRealSecretsOrLocalPaths() throws IOException {
        assertSecretSafe(projectText("docs/agent/TOOL_CONTRACTS.md"));
        assertSecretSafe(projectText("docs/agent/RAG_POLICY_RETRIEVAL_CONTRACT.md"));
        assertSecretSafe(projectText("docs/exec-plans/completed/EXEC_PLAN_V4_RAG_SEARCH_CONTRACT.md"));
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
