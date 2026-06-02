package io.github.tatame.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.tatame.aftersale.agent.infrastructure.springai.SpringAiChatGateway;
import io.github.tatame.aftersale.policy.rag.domain.PolicyVectorRepository;
import io.github.tatame.aftersale.policy.rag.infrastructure.pgvector.JdbcPolicyVectorRepository;
import io.github.tatame.aftersale.policy.rag.infrastructure.pgvector.PgVectorProfileGuard;
import io.github.tatame.aftersale.policy.rag.infrastructure.springai.SpringAiEmbeddingGateway;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "agent.rag.health.include-details=true",
        "management.endpoint.health.show-details=always",
        "management.health.diskspace.enabled=false"
})
class DefaultOfflineValidationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private HealthEndpoint healthEndpoint;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void defaultContextStartsWithoutDatabaseVectorStoreOrLiveProviderBeans() {
        assertThat(applicationContext.getBeansOfType(DataSource.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(PgVectorProfileGuard.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(PolicyVectorRepository.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(JdbcPolicyVectorRepository.class)).isEmpty();
        assertThat(beanNamesForOptionalType("org.springframework.ai.chat.model.ChatModel")).isEmpty();
        assertThat(beanNamesForOptionalType("org.springframework.ai.embedding.EmbeddingModel")).isEmpty();
        assertThat(beanNamesForOptionalType("org.springframework.ai.vectorstore.VectorStore")).isEmpty();
        assertThat(applicationContext.getBeansOfType(SpringAiChatGateway.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(SpringAiEmbeddingGateway.class)).isEmpty();
    }

    @Test
    void defaultHealthReadinessDoesNotExecuteLiveChecks() throws Exception {
        assertThat(healthEndpoint.health().getStatus().getCode()).isEqualTo("UP");

        MvcResult result = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains(
                "\"offlineReadinessOnly\":true",
                "\"databaseConnectionAttempted\":false",
                "\"vectorSearchExecuted\":false",
                "\"embeddingCallExecuted\":false",
                "\"springAiCallExecuted\":false");
        assertThat(body).doesNotContain("apiKey");
        assertThat(body).doesNotContain("password=");
        assertThat(body).doesNotContain("token=");
        assertThat(body).doesNotContain("D:\\");
        assertThat(body).doesNotContain("/Users/");
    }

    @Test
    void defaultActuatorExposureKeepsOnlyHealthAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/configprops"))
                .andExpect(status().isNotFound());
    }

    private String[] beanNamesForOptionalType(String className) {
        try {
            return applicationContext.getBeanNamesForType(Class.forName(className));
        } catch (ClassNotFoundException exception) {
            return new String[0];
        }
    }
}
