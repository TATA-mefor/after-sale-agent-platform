package com.example.aftersale.policy.rag.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "agent.rag.health.include-details=true",
        "management.health.diskspace.enabled=false",
        "management.endpoint.health.show-details=always"
})
class RagActuatorHealthEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HealthEndpoint healthEndpoint;

    @Test
    void healthEndpointIncludesRagReadinessWithoutSecrets() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains(
                "ragSearch",
                "ragVectorStore",
                "ragEmbedding",
                "ragIngestion",
                "offlineReadinessOnly",
                "databaseConnectionAttempted",
                "embeddingCallExecuted");
        assertThat(body).doesNotContain("apiKey");
        assertThat(body).doesNotContain("password=");
        assertThat(body).doesNotContain("token=");
        assertThat(body).doesNotContain("D:\\");
        assertThat(body).doesNotContain("/Users/");
    }

    @Test
    void defaultContextCreatesRagHealthIndicatorsWithoutExternalDependencies() {
        assertThat(healthEndpoint.health().getStatus().getCode()).isEqualTo("UP");
    }
}
