package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.aftersale.agent.application.workspace.AgentWorkspace;
import java.time.Instant;
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
}
