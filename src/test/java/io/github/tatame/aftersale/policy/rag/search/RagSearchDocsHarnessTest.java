package io.github.tatame.aftersale.policy.rag.search;

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
    void ragMergeServiceDocsDescribeV452Boundary() throws IOException {
        String docs = projectText("docs/agent/TOOL_CONTRACTS.md")
                + "\n"
                + projectText("docs/agent/RAG_POLICY_RETRIEVAL_CONTRACT.md")
                + "\n"
                + projectText("README.md")
                + "\n"
                + projectText("docs/quality/QUALITY_SCORE.md")
                + "\n"
                + projectText("docs/exec-plans/completed/EXEC_PLAN_V4_RAG_EVIDENCE_MERGE_SERVICE.md");

        assertThat(docs).contains("V4.5.2");
        assertThat(docs).contains("keyword + vector merge service");
        assertThat(docs).contains("score merge");
        assertThat(docs).contains("dedup");
        assertThat(docs).contains("fallback");
        assertThat(docs).contains("does not change `search_aftersale_policy` runtime");
        assertThat(docs).contains("does not call `EmbeddingClient`");
        assertThat(docs).contains("does not call `PolicyVectorRepository.search`");
        assertThat(docs).contains("V4.5.3");
        assertThat(docs).contains("evidence only");
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
    void ragMergeServiceCompletionRecordExists() throws IOException {
        String completionRecord = projectText("docs/exec-plans/completed/EXEC_PLAN_V4_RAG_EVIDENCE_MERGE_SERVICE.md");

        assertThat(completionRecord).contains("Status: Completed");
        assertThat(completionRecord).contains("Merge Service Boundary");
        assertThat(completionRecord).contains("Score Merge Boundary");
        assertThat(completionRecord).contains("Dedup Boundary");
        assertThat(completionRecord).contains("Fallback Boundary");
        assertThat(completionRecord).contains("TASK_COMPLETE");
    }

    @Test
    void ragHybridRuntimeDocsDescribeV453Boundary() throws IOException {
        String docs = projectText("docs/agent/TOOL_CONTRACTS.md")
                + "\n"
                + projectText("docs/agent/RAG_POLICY_RETRIEVAL_CONTRACT.md")
                + "\n"
                + projectText("README.md")
                + "\n"
                + projectText("docs/quality/QUALITY_SCORE.md")
                + "\n"
                + projectText("docs/exec-plans/completed/EXEC_PLAN_V4_SEARCH_AFTERSALE_POLICY_HYBRID_RUNTIME.md");

        assertThat(docs).contains("V4.5.3");
        assertThat(docs).contains("search_aftersale_policy");
        assertThat(docs).contains("supports KEYWORD / VECTOR / HYBRID");
        assertThat(docs).contains("defaults to KEYWORD");
        assertThat(docs).contains("FakeEmbeddingClient");
        assertThat(docs).contains("InMemoryPolicyVectorRepository");
        assertThat(docs).contains("real PGvector");
        assertThat(docs).contains("real embedding providers");
        assertThat(docs).contains("Spring AI `VectorStore`");
        assertThat(docs).contains("LOW-risk read-only");
        assertThat(docs).contains("evidence only");
        assertThat(docs).contains("V4.5.4 handles ToolCallTrace / Workspace evidence");
        assertThat(docs).contains("does not require real LLMs, API keys, PostgreSQL, PGvector, Docker");
        assertThat(docs).contains("external network");
    }

    @Test
    void ragHybridRuntimeCompletionRecordExists() throws IOException {
        String completionRecord =
                projectText("docs/exec-plans/completed/EXEC_PLAN_V4_SEARCH_AFTERSALE_POLICY_HYBRID_RUNTIME.md");

        assertThat(completionRecord).contains("Status: Completed");
        assertThat(completionRecord).contains("Runtime Mode Boundary");
        assertThat(completionRecord).contains("KEYWORD Mode Boundary");
        assertThat(completionRecord).contains("VECTOR Mode Boundary");
        assertThat(completionRecord).contains("HYBRID Mode Boundary");
        assertThat(completionRecord).contains("ToolRegistry Boundary");
        assertThat(completionRecord).contains("Evidence-only Boundary");
        assertThat(completionRecord).contains("TASK_COMPLETE");
    }

    @Test
    void ragTraceWorkspaceEvidenceDocsDescribeV454Boundary() throws IOException {
        String docs = projectText("docs/agent/TOOL_CONTRACTS.md")
                + "\n"
                + projectText("docs/agent/RAG_POLICY_RETRIEVAL_CONTRACT.md")
                + "\n"
                + projectText("README.md")
                + "\n"
                + projectText("docs/quality/QUALITY_SCORE.md")
                + "\n"
                + projectText("docs/exec-plans/completed/EXEC_PLAN_V4_RAG_TRACE_WORKSPACE_EVIDENCE.md");

        assertThat(docs).contains("V4.5.4");
        assertThat(docs).contains("ToolCallTrace / Workspace evidence");
        assertThat(docs).contains("ToolCallTrace output JSON");
        assertThat(docs).contains("AgentWorkspace");
        assertThat(docs).contains("Execution Tree");
        assertThat(docs).contains("final summary");
        assertThat(docs).contains("LOW-risk read-only");
        assertThat(docs).contains("evidence only");
        assertThat(docs).contains("does not change retrieval algorithms");
        assertThat(docs).contains("does not require real LLMs, API keys, PostgreSQL, PGvector, Docker");
        assertThat(docs).contains("external network");
        assertThat(docs).contains("real PGvector and real embedding providers remain opt-in");
    }

    @Test
    void ragTraceWorkspaceEvidenceCompletionRecordExists() throws IOException {
        String completionRecord =
                projectText("docs/exec-plans/completed/EXEC_PLAN_V4_RAG_TRACE_WORKSPACE_EVIDENCE.md");

        assertThat(completionRecord).contains("Status: Completed");
        assertThat(completionRecord).contains("ToolCallTrace Evidence Boundary");
        assertThat(completionRecord).contains("Workspace Evidence Boundary");
        assertThat(completionRecord).contains("Final Summary Boundary");
        assertThat(completionRecord).contains("Execution Tree Evidence Boundary");
        assertThat(completionRecord).contains("Evidence-only Boundary");
        assertThat(completionRecord).contains("Default Offline Test Boundary");
        assertThat(completionRecord).contains("Architecture Boundary");
        assertThat(completionRecord).contains("TASK_COMPLETE");
    }

    @Test
    void ragSearchDocsDoNotContainRealSecretsOrLocalPaths() throws IOException {
        assertSecretSafe(projectText("docs/agent/TOOL_CONTRACTS.md"));
        assertSecretSafe(projectText("docs/agent/RAG_POLICY_RETRIEVAL_CONTRACT.md"));
        assertSecretSafe(projectText("docs/exec-plans/completed/EXEC_PLAN_V4_RAG_SEARCH_CONTRACT.md"));
        assertSecretSafe(projectText("docs/exec-plans/completed/EXEC_PLAN_V4_RAG_EVIDENCE_MERGE_SERVICE.md"));
        assertSecretSafe(
                projectText("docs/exec-plans/completed/EXEC_PLAN_V4_SEARCH_AFTERSALE_POLICY_HYBRID_RUNTIME.md"));
        assertSecretSafe(projectText("docs/exec-plans/completed/EXEC_PLAN_V4_RAG_TRACE_WORKSPACE_EVIDENCE.md"));
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
