package io.github.tatame.aftersale.common.observability.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class ApplicationMetricsRecorderTest {

    @Test
    void recordsAgentToolApprovalRagAndProviderMeters() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ApplicationMetricsRecorder recorder = new ApplicationMetricsRecorder(meterRegistry);
        long startedAt = System.nanoTime();

        recorder.recordAgentRunStarted();
        recorder.recordAgentRunCompleted("SUCCEEDED", MetricOutcome.SUCCEEDED, startedAt);
        recorder.recordToolCall("search_aftersale_policy", "SUCCEEDED", "LOW", startedAt);
        recorder.recordApprovalRequest("HIGH");
        recorder.recordApprovalDecision("APPROVED");
        recorder.recordRagSearch("HYBRID", false, MetricOutcome.SUCCEEDED, startedAt);
        recorder.recordProviderCall("fake", "chat", MetricOutcome.SUCCEEDED, startedAt);

        assertCounter(meterRegistry, MetricNames.AGENT_RUN_TOTAL, "outcome", "started", 1.0d);
        assertCounter(meterRegistry, MetricNames.AGENT_RUN_TOTAL, "outcome", "succeeded", 1.0d);
        assertTimer(meterRegistry, MetricNames.AGENT_RUN_DURATION, "status", "succeeded");
        assertCounter(meterRegistry, MetricNames.TOOL_CALL_TOTAL, "tool_name", "search_aftersale_policy", 1.0d);
        assertTimer(meterRegistry, MetricNames.TOOL_CALL_DURATION, "risk_level", "low");
        assertCounter(meterRegistry, MetricNames.APPROVAL_REQUEST_TOTAL, "risk_level", "high", 1.0d);
        assertCounter(meterRegistry, MetricNames.APPROVAL_DECISION_TOTAL, "approval_decision", "approved", 1.0d);
        assertCounter(meterRegistry, MetricNames.RAG_SEARCH_TOTAL, "retrieval_mode", "hybrid", 1.0d);
        assertTimer(meterRegistry, MetricNames.RAG_SEARCH_DURATION, "fallback", "false");
        assertCounter(meterRegistry, MetricNames.PROVIDER_CALL_TOTAL, "provider_type", "fake", 1.0d);
        assertTimer(meterRegistry, MetricNames.PROVIDER_CALL_DURATION, "operation", "chat");
    }

    @Test
    void sanitizesHighCardinalityOrSensitiveTagValues() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ApplicationMetricsRecorder recorder = new ApplicationMetricsRecorder(meterRegistry);
        long startedAt = System.nanoTime();

        recorder.recordToolCall(
                "search query apiKey=sk-secret",
                "SUCCEEDED",
                "LOW",
                startedAt);
        recorder.recordProviderCall("https://provider.example.test/path", "raw prompt", MetricOutcome.FAILED,
                startedAt);

        assertCounter(meterRegistry, MetricNames.TOOL_CALL_TOTAL, "tool_name", "unknown", 1.0d);
        assertCounter(meterRegistry, MetricNames.PROVIDER_CALL_TOTAL, "provider_type", "unknown", 1.0d);
        assertTimer(meterRegistry, MetricNames.PROVIDER_CALL_DURATION, "operation", "unknown");
    }

    @Test
    void recorderDoesNotThrowWhenMeterRegistryFails() {
        ApplicationMetricsRecorder recorder = new ApplicationMetricsRecorder(mock(MeterRegistry.class));

        assertThatCode(() -> {
            recorder.recordAgentRunStarted();
            recorder.recordToolCall("search_aftersale_policy", "SUCCEEDED", "LOW", System.nanoTime());
            recorder.recordRagSearch("KEYWORD", false, MetricOutcome.SUCCEEDED, System.nanoTime());
        }).doesNotThrowAnyException();
    }

    private static void assertCounter(
            SimpleMeterRegistry meterRegistry,
            String name,
            String tagKey,
            String tagValue,
            double expectedCount) {
        Counter counter = meterRegistry.find(name).tag(tagKey, tagValue).counter();
        assertThat(counter).as(name + " counter").isNotNull();
        assertThat(counter.count()).isEqualTo(expectedCount);
    }

    private static void assertTimer(
            SimpleMeterRegistry meterRegistry,
            String name,
            String tagKey,
            String tagValue) {
        Timer timer = meterRegistry.find(name).tag(tagKey, tagValue).timer();
        assertThat(timer).as(name + " timer").isNotNull();
        assertThat(timer.count()).isEqualTo(1L);
    }
}
