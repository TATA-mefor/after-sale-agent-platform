package io.github.tatame.aftersale.common.observability.correlation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.tatame.aftersale.agent.infrastructure.springai.SpringAiChatGateway;
import io.github.tatame.aftersale.common.observability.metrics.ApplicationMetricsRecorder;
import io.github.tatame.aftersale.common.observability.metrics.MetricOutcome;
import io.github.tatame.aftersale.policy.rag.domain.PolicyVectorRepository;
import io.github.tatame.aftersale.policy.rag.infrastructure.pgvector.JdbcPolicyVectorRepository;
import io.github.tatame.aftersale.policy.rag.infrastructure.pgvector.PgVectorProfileGuard;
import io.github.tatame.aftersale.policy.rag.infrastructure.springai.SpringAiEmbeddingGateway;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "management.health.diskspace.enabled=false"
})
class CorrelationObservabilityBoundaryTest {

    private static final List<String> UNEXPOSED_ENDPOINTS = List.of(
            "/actuator/metrics",
            "/actuator/prometheus",
            "/actuator/env",
            "/actuator/beans",
            "/actuator/configprops",
            "/actuator/heapdump",
            "/actuator/threaddump");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void correlationFilterExistsWithoutBroadActuatorExposure() throws Exception {
        assertThat(applicationContext.getBeansOfType(CorrelationIdFilter.class)).hasSize(1);
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        for (String endpoint : UNEXPOSED_ENDPOINTS) {
            mockMvc.perform(get(endpoint)).andExpect(status().isNotFound());
        }
    }

    @Test
    void defaultContextDoesNotCreateLiveDependenciesOrOpenTelemetryBeans() {
        assertThat(applicationContext.getBeansOfType(DataSource.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(PgVectorProfileGuard.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(PolicyVectorRepository.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(JdbcPolicyVectorRepository.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(SpringAiChatGateway.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(SpringAiEmbeddingGateway.class)).isEmpty();
        assertThat(beanNamesForOptionalType("org.springframework.ai.chat.model.ChatModel")).isEmpty();
        assertThat(beanNamesForOptionalType("org.springframework.ai.embedding.EmbeddingModel")).isEmpty();
        assertThat(beanNamesForOptionalType("org.springframework.ai.vectorstore.VectorStore")).isEmpty();
        assertThat(beanNamesForOptionalType("io.opentelemetry.api.trace.Tracer")).isEmpty();
        assertThat(beanNamesForOptionalType("io.opentelemetry.sdk.trace.export.SpanExporter")).isEmpty();
    }

    @Test
    void metricsTagsDoNotUseCorrelationOrRequestIds() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ApplicationMetricsRecorder recorder = new ApplicationMetricsRecorder(meterRegistry);

        recorder.recordAgentRunStarted();
        recorder.recordAgentRunCompleted("COMPLETED", MetricOutcome.SUCCEEDED, System.nanoTime());
        recorder.recordToolCall("search_aftersale_policy", "SUCCESS", "LOW", System.nanoTime());
        recorder.recordApprovalRequest("HIGH");
        recorder.recordApprovalDecision("APPROVED");
        recorder.recordRagSearch("HYBRID", false, MetricOutcome.SUCCEEDED, System.nanoTime());
        recorder.recordProviderCall("fake", "embed", MetricOutcome.SUCCEEDED, System.nanoTime());

        for (Meter meter : meterRegistry.getMeters()) {
            assertThat(meter.getId().getTag(CorrelationHeaders.CORRELATION_ID_MDC_KEY)).isNull();
            assertThat(meter.getId().getTag(CorrelationHeaders.REQUEST_ID_MDC_KEY)).isNull();
        }
    }

    private String[] beanNamesForOptionalType(String className) {
        try {
            return applicationContext.getBeanNamesForType(Class.forName(className));
        } catch (ClassNotFoundException exception) {
            return new String[0];
        }
    }
}
