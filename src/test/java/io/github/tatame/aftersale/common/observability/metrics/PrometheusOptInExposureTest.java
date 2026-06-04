package io.github.tatame.aftersale.common.observability.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.tatame.aftersale.agent.infrastructure.springai.SpringAiChatGateway;
import io.github.tatame.aftersale.policy.rag.domain.PolicyVectorRepository;
import io.github.tatame.aftersale.policy.rag.infrastructure.pgvector.JdbcPolicyVectorRepository;
import io.github.tatame.aftersale.policy.rag.infrastructure.pgvector.PgVectorProfileGuard;
import io.github.tatame.aftersale.policy.rag.infrastructure.springai.SpringAiEmbeddingGateway;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class PrometheusOptInExposureTest {

    private static final List<String> SENSITIVE_ENDPOINTS = List.of(
            "/actuator/env",
            "/actuator/beans",
            "/actuator/configprops",
            "/actuator/heapdump",
            "/actuator/threaddump");

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @TestPropertySource(properties = {
            "management.health.diskspace.enabled=false",
            "management.metrics.enable.disk=false"
    })
    class DefaultProfileExposure {

        @Autowired
        private ApplicationContext applicationContext;

        @Autowired
        private MockMvc mockMvc;

        @Test
        void defaultProfileKeepsPrometheusAndMetricsEndpointsUnavailable() throws Exception {
            mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
            mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isNotFound());
            mockMvc.perform(get("/actuator/metrics")).andExpect(status().isNotFound());

            for (String endpoint : SENSITIVE_ENDPOINTS) {
                mockMvc.perform(get(endpoint)).andExpect(status().isNotFound());
            }
        }

        @Test
        void defaultProfileDoesNotCreateLiveDependencyBeans() {
            assertNoLiveDependencyBeans(applicationContext);
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("observability-prometheus")
    @TestPropertySource(properties = {
            "management.health.diskspace.enabled=false",
            "management.metrics.enable.disk=false"
    })
    class OptInProfileExposure {

        @Autowired
        private ApplicationContext applicationContext;

        @Autowired
        private MockMvc mockMvc;

        @Test
        void optInProfileExposesOnlyHealthAndPrometheus() throws Exception {
            mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
            mockMvc.perform(get("/actuator/metrics")).andExpect(status().isNotFound());

            for (String endpoint : SENSITIVE_ENDPOINTS) {
                mockMvc.perform(get(endpoint)).andExpect(status().isNotFound());
            }

            MvcResult result = mockMvc.perform(get("/actuator/prometheus"))
                    .andExpect(status().isOk())
                    .andReturn();
            String body = result.getResponse().getContentAsString();

            assertThat(applicationContext.getBeansOfType(PrometheusMeterRegistry.class)).isNotEmpty();
            assertThat(body).contains("# HELP", "# TYPE");
            assertSafePrometheusBody(body);
        }

        @Test
        void optInProfileStillDoesNotCreateLiveDependencyBeans() {
            assertNoLiveDependencyBeans(applicationContext);
        }
    }

    private static void assertNoLiveDependencyBeans(ApplicationContext applicationContext) {
        assertThat(applicationContext.getBeansOfType(DataSource.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(PgVectorProfileGuard.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(PolicyVectorRepository.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(JdbcPolicyVectorRepository.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(SpringAiChatGateway.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(SpringAiEmbeddingGateway.class)).isEmpty();
        assertThat(beanNamesForOptionalType(applicationContext, "org.springframework.ai.chat.model.ChatModel"))
                .isEmpty();
        assertThat(beanNamesForOptionalType(applicationContext, "org.springframework.ai.embedding.EmbeddingModel"))
                .isEmpty();
        assertThat(beanNamesForOptionalType(applicationContext, "org.springframework.ai.vectorstore.VectorStore"))
                .isEmpty();
    }

    private static void assertSafePrometheusBody(String body) {
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

    private static String[] beanNamesForOptionalType(ApplicationContext applicationContext, String className) {
        try {
            return applicationContext.getBeanNamesForType(Class.forName(className));
        } catch (ClassNotFoundException exception) {
            return new String[0];
        }
    }
}
