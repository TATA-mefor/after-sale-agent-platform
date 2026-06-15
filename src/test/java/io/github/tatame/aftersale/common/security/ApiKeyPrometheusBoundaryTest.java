package io.github.tatame.aftersale.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"security-api-key", "observability-prometheus"})
@TestPropertySource(properties = {
        "agent.security.api-keys.admin=admin-test-key",
        "agent.security.api-keys.supervisor=supervisor-test-key",
        "agent.security.api-keys.operator=operator-test-key",
        "agent.security.api-keys.system-service=system-test-key",
        "management.health.diskspace.enabled=false"
})
class ApiKeyPrometheusBoundaryTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void prometheusEndpointRequiresAdminOrSystemServiceWhenExposed() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/prometheus").header("X-API-Key", "operator-test-key"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/prometheus").header("X-API-Key", "system-test-key"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/prometheus").header("X-API-Key", "admin-test-key"))
                .andExpect(status().isOk());
    }
}
