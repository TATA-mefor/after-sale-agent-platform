package io.github.tatame.aftersale.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("security-api-key")
@TestPropertySource(properties = {
        "agent.security.api-keys.admin=admin-test-key",
        "agent.security.api-keys.supervisor=supervisor-test-key",
        "agent.security.api-keys.operator=operator-test-key",
        "agent.security.api-keys.system-service=system-test-key",
        "management.health.diskspace.enabled=false"
})
class ApiKeyAuthBoundaryTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedApiRejectsMissingOrWrongApiKeyWithoutSecretLeakage() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isUnauthorized());

        MvcResult wrongKeyResult = mockMvc.perform(get("/api/health")
                        .header("X-API-Key", "wrong-test-key"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(wrongKeyResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .doesNotContain("wrong-test-key")
                .doesNotContain("admin-test-key")
                .doesNotContain("operator-test-key");
    }

    @Test
    void actuatorHealthProbesRemainPublicUnderSecurityProfile() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    @Test
    void ticketAndAgentRunApisAcceptApplicationRoles() throws Exception {
        mockMvc.perform(get("/api/tickets").header("X-API-Key", "operator-test-key"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/tickets")
                        .header("X-API-Key", "system-test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "U-SEC-KEY-1",
                                  "orderId": "O-SEC-KEY-1",
                                  "message": "System service can create tickets through the protected API."
                                }
                                """))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/tickets/T-SEC-MISSING/agent-runs")
                        .header("X-API-Key", "system-test-key"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    @Test
    void approvalDecisionEndpointsRequireSupervisorOrAdmin() throws Exception {
        mockMvc.perform(post("/api/approval-requests/APP-SEC-MISSING/approve")
                        .header("X-API-Key", "operator-test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewerId": "reviewer-sec-1",
                                  "reason": "Role boundary test."
                                }
                                """))
                .andExpect(status().isForbidden());

        MvcResult supervisorResult = mockMvc.perform(post("/api/approval-requests/APP-SEC-MISSING/approve")
                        .header("X-API-Key", "supervisor-test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewerId": "reviewer-sec-2",
                                  "reason": "Role boundary test."
                                }
                                """))
                .andReturn();
        assertThat(supervisorResult.getResponse().getStatus()).isNotIn(401, 403);
    }

    @Test
    void traceAndExecutionTreeReadDenySystemServiceButAllowOperator() throws Exception {
        mockMvc.perform(get("/api/agent-runs/RUN-SEC-MISSING/traces")
                        .header("X-API-Key", "system-test-key"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/agent-runs/RUN-SEC-MISSING/execution-tree")
                        .header("X-API-Key", "system-test-key"))
                .andExpect(status().isForbidden());

        MvcResult traceResult = mockMvc.perform(get("/api/agent-runs/RUN-SEC-MISSING/traces")
                        .header("X-API-Key", "operator-test-key"))
                .andReturn();
        MvcResult treeResult = mockMvc.perform(get("/api/agent-runs/RUN-SEC-MISSING/execution-tree")
                        .header("X-API-Key", "operator-test-key"))
                .andReturn();

        assertThat(traceResult.getResponse().getStatus()).isNotIn(401, 403);
        assertThat(treeResult.getResponse().getStatus()).isNotIn(401, 403);
    }

    @Test
    void openApiAndSwaggerRequireSupervisorOrAdmin() throws Exception {
        mockMvc.perform(get("/v3/api-docs").header("X-API-Key", "operator-test-key"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/swagger-ui.html").header("X-API-Key", "operator-test-key"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v3/api-docs").header("X-API-Key", "supervisor-test-key"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui.html").header("X-API-Key", "admin-test-key"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void sensitiveActuatorEndpointsRemainUnexposed() throws Exception {
        for (String endpoint : new String[] {
                "/actuator/env",
                "/actuator/beans",
                "/actuator/configprops",
                "/actuator/heapdump",
                "/actuator/threaddump"}) {
            mockMvc.perform(get(endpoint).header("X-API-Key", "admin-test-key"))
                    .andExpect(status().isNotFound());
        }
    }
}
