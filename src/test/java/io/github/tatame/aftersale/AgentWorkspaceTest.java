package io.github.tatame.aftersale;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.tatame.aftersale.agent.application.workspace.AgentWorkspace;
import io.github.tatame.aftersale.agent.application.workspace.PolicyEvidence;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentWorkspaceTest {

    @Test
    void workspaceStartsWithAgentRunAndTicketIds() {
        Instant createdAt = Instant.parse("2026-05-16T08:00:00Z");

        AgentWorkspace workspace = AgentWorkspace.start("RUN-WORKSPACE-1", "T-WORKSPACE-1", createdAt);

        assertThat(workspace.agentRunId()).isEqualTo("RUN-WORKSPACE-1");
        assertThat(workspace.ticketId()).isEqualTo("T-WORKSPACE-1");
        assertThat(workspace.createdAt()).isEqualTo(createdAt);
        assertThat(workspace.orderFacts()).isEmpty();
        assertThat(workspace.policyEvidence()).isEmpty();
        assertThat(workspace.subtaskMemories()).isEmpty();
        assertThat(workspace.toolResultSummaries()).isEmpty();
        assertThat(workspace.riskFlags()).isEmpty();
    }

    @Test
    void policyEvidenceMapsRagEvidenceFromToolOutput() {
        List<PolicyEvidence> evidence = PolicyEvidence.fromToolData(
                "search_aftersale_policy",
                "subtask-1",
                Map.of("evidences", List.of(Map.of(
                        "evidenceId", "evidence-1",
                        "documentId", "doc-1",
                        "chunkId", "chunk-1",
                        "documentTitle", "Quality Return Policy",
                        "category", "质量问题退换货规则",
                        "productType", "通用商品",
                        "snippet", "商品存在质量问题时，可申请退货、退款或换货。",
                        "score", 0.82d,
                        "retrievalMode", "HYBRID",
                        "source", "MERGED_HYBRID"))));

        assertThat(evidence).hasSize(1);
        PolicyEvidence first = evidence.get(0);
        assertThat(first.evidenceId()).isEqualTo("evidence-1");
        assertThat(first.documentId()).isEqualTo("doc-1");
        assertThat(first.chunkId()).isEqualTo("chunk-1");
        assertThat(first.documentTitle()).isEqualTo("Quality Return Policy");
        assertThat(first.score()).isEqualTo(0.82d);
        assertThat(first.retrievalMode()).isEqualTo("HYBRID");
        assertThat(first.source()).isEqualTo("MERGED_HYBRID");
        assertThat(first.summary()).contains("Policy evidence[HYBRID]", "chunk=chunk-1", "score=0.82");
    }

    @Test
    void policyEvidenceFallsBackToLegacyResultsAndKeepsKeywordCompatibility() {
        List<PolicyEvidence> evidence = PolicyEvidence.fromToolData(
                "search_aftersale_policy",
                "",
                Map.of("results", List.of(Map.of(
                        "policyId", "POL-QUALITY-RETURN-EXCHANGE",
                        "category", "质量问题退换货规则",
                        "matchedText", "质量问题商品可按政策申请售后。",
                        "matchReason", "Matched controlled keyword set"))));

        assertThat(evidence).hasSize(1);
        assertThat(evidence.get(0).policyId()).isEqualTo("POL-QUALITY-RETURN-EXCHANGE");
        assertThat(evidence.get(0).retrievalMode()).isBlank();
        assertThat(evidence.get(0).summary()).contains("Policy evidence[KEYWORD]");
    }

    @Test
    void policyEvidenceDoesNotFabricateEmptyEvidenceAndSanitizesText() {
        List<PolicyEvidence> emptyEvidence = PolicyEvidence.fromToolData(
                "search_aftersale_policy",
                "",
                Map.of("evidences", List.of()));
        List<PolicyEvidence> sanitizedEvidence = PolicyEvidence.fromToolData(
                "search_aftersale_policy",
                "",
                Map.of("evidences", List.of(Map.of(
                        "category", "质量问题 password=hunter2 token=secret",
                        "snippet", "sk-testvalue password=secret token=abc D:\\secret\\raw.txt "
                                + "raw text ".repeat(40),
                        "source", "VECTOR_CHUNK",
                        "retrievalMode", "VECTOR"))));

        assertThat(emptyEvidence).isEmpty();
        assertThat(sanitizedEvidence).hasSize(1);
        String summary = sanitizedEvidence.get(0).summary();
        assertThat(summary)
                .doesNotContain("sk-testvalue", "hunter2", "token=secret", "D:\\secret\\raw.txt")
                .contains("sk-***", "password=***", "token=***", "[local-path]");
        assertThat(sanitizedEvidence.get(0).snippet()).hasSizeLessThanOrEqualTo(180);
    }
}
