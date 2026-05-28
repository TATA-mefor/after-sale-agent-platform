package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDocsExposeCurrentApiSurfaceWithoutSecrets() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains(
                "\"title\":\"AfterSale-Agent API\"",
                "\"version\":\"V4\"",
                "ToolRegistry-controlled",
                "KEYWORD / VECTOR / HYBRID",
                "/api/health",
                "/api/tickets",
                "/api/tickets/{ticketId}",
                "/api/tickets/{ticketId}/agent-runs",
                "/api/agent-runs/{runId}/traces",
                "/api/agent-runs/{runId}/execution-tree",
                "/api/approval-requests/pending",
                "/api/approval-requests/{approvalRequestId}",
                "/api/approval-requests/{approvalRequestId}/approve",
                "/api/approval-requests/{approvalRequestId}/reject",
                "Tickets",
                "Agent Runs",
                "Approvals",
                "Execution Tree",
                "Tool Traces");
        assertSecretSafe(body);
    }

    @Test
    void swaggerUiAndHealthAreAvailableWithoutBroadActuatorExposure() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isNotFound());
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
        assertThat(lower).doesNotContain("full prompt");
    }
}
