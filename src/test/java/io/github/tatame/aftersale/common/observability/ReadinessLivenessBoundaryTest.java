package io.github.tatame.aftersale.common.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.tatame.aftersale.agent.infrastructure.springai.SpringAiChatGateway;
import io.github.tatame.aftersale.policy.rag.domain.PolicyVectorRepository;
import io.github.tatame.aftersale.policy.rag.infrastructure.pgvector.JdbcPolicyVectorRepository;
import io.github.tatame.aftersale.policy.rag.infrastructure.pgvector.PgVectorProfileGuard;
import io.github.tatame.aftersale.policy.rag.infrastructure.springai.SpringAiEmbeddingGateway;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "management.health.diskspace.enabled=false"
})
class ReadinessLivenessBoundaryTest {

    private static final List<String> SENSITIVE_ENDPOINTS = List.of(
            "/actuator/env",
            "/actuator/beans",
            "/actuator/configprops",
            "/actuator/heapdump",
            "/actuator/threaddump",
            "/actuator/prometheus");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthLivenessAndReadinessEndpointsAreAvailable() throws Exception {
        assertSafeHealthBody(getBody("/actuator/health"));
        assertSafeHealthBody(getBody("/actuator/health/liveness"));
        assertSafeHealthBody(getBody("/actuator/health/readiness"));
    }

    @Test
    void sensitiveActuatorEndpointsRemainUnexposedByDefault() throws Exception {
        for (String endpoint : SENSITIVE_ENDPOINTS) {
            mockMvc.perform(get(endpoint)).andExpect(status().isNotFound());
        }
    }

    @Test
    void defaultContextDoesNotCreateLiveDependencyBeans() {
        assertThat(applicationContext.getBeansOfType(DataSource.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(PgVectorProfileGuard.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(PolicyVectorRepository.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(JdbcPolicyVectorRepository.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(SpringAiChatGateway.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(SpringAiEmbeddingGateway.class)).isEmpty();
        assertThat(beanNamesForOptionalType("org.springframework.ai.chat.model.ChatModel")).isEmpty();
        assertThat(beanNamesForOptionalType("org.springframework.ai.embedding.EmbeddingModel")).isEmpty();
        assertThat(beanNamesForOptionalType("org.springframework.ai.vectorstore.VectorStore")).isEmpty();
    }

    private String getBody(String endpoint) throws Exception {
        MvcResult result = mockMvc.perform(get(endpoint))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private static void assertSafeHealthBody(String body) {
        String lower = body.toLowerCase(Locale.ROOT);
        assertThat(lower).doesNotContain(
                "api_key",
                "apikey",
                "api key",
                "openai_api_key",
                "dashscope_api_key",
                "spring_ai_openai_api_key",
                "password",
                "token",
                "secret",
                "jdbc:",
                "d:/",
                "d:\\",
                "c:/",
                "c:\\",
                "/users/",
                "/home/",
                "raw prompt",
                "raw provider response");
    }

    private String[] beanNamesForOptionalType(String className) {
        try {
            return applicationContext.getBeanNamesForType(Class.forName(className));
        } catch (ClassNotFoundException exception) {
            return new String[0];
        }
    }
}
