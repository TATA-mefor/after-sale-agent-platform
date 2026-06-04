package io.github.tatame.aftersale.common.observability.metrics;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Central application metrics boundary for Agent, Tool, Approval, RAG, and provider observations.
 *
 * <p>Metric recording is best-effort. Recorder methods intentionally do not throw into business runtime paths.
 */
@Component
public class ApplicationMetricsRecorder {

    private static final String AGENT_COMPONENT = "agent";
    private static final String APPROVAL_COMPONENT = "approval";
    private static final String PROVIDER_COMPONENT = "provider";
    private static final String RAG_COMPONENT = "rag";
    private static final String TOOL_COMPONENT = "tool";

    private final MeterRegistry meterRegistry;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores application collaborators.")
    public ApplicationMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordAgentRunStarted() {
        recordCounter(MetricNames.AGENT_RUN_TOTAL, Tags.of(
                MetricTags.COMPONENT, AGENT_COMPONENT,
                MetricTags.OPERATION, "run",
                MetricTags.OUTCOME, MetricTagSanitizer.sanitize(MetricOutcome.STARTED),
                MetricTags.STATUS, "running"));
    }

    public void recordAgentRunCompleted(String status, MetricOutcome outcome, long startedAtNanos) {
        Tags tags = Tags.of(
                MetricTags.COMPONENT, AGENT_COMPONENT,
                MetricTags.OPERATION, "run",
                MetricTags.OUTCOME, MetricTagSanitizer.sanitize(outcome),
                MetricTags.STATUS, MetricTagSanitizer.sanitize(status));
        recordCounter(MetricNames.AGENT_RUN_TOTAL, tags);
        recordTimer(MetricNames.AGENT_RUN_DURATION, tags, startedAtNanos);
    }

    public void recordToolCall(String toolName, String status, String riskLevel, long startedAtNanos) {
        Tags tags = Tags.of(
                MetricTags.COMPONENT, TOOL_COMPONENT,
                MetricTags.OPERATION, "execute",
                MetricTags.OUTCOME, MetricTagSanitizer.sanitize(status),
                MetricTags.STATUS, MetricTagSanitizer.sanitize(status),
                MetricTags.TOOL_NAME, MetricTagSanitizer.sanitize(toolName),
                MetricTags.RISK_LEVEL, MetricTagSanitizer.sanitize(riskLevel));
        recordCounter(MetricNames.TOOL_CALL_TOTAL, tags);
        recordTimer(MetricNames.TOOL_CALL_DURATION, tags, startedAtNanos);
    }

    public void recordApprovalRequest(String riskLevel) {
        recordCounter(MetricNames.APPROVAL_REQUEST_TOTAL, Tags.of(
                MetricTags.COMPONENT, APPROVAL_COMPONENT,
                MetricTags.OPERATION, "request",
                MetricTags.OUTCOME, MetricTagSanitizer.sanitize(MetricOutcome.REQUESTED),
                MetricTags.RISK_LEVEL, MetricTagSanitizer.sanitize(riskLevel)));
    }

    public void recordApprovalDecision(String decision) {
        recordCounter(MetricNames.APPROVAL_DECISION_TOTAL, Tags.of(
                MetricTags.COMPONENT, APPROVAL_COMPONENT,
                MetricTags.OPERATION, "decision",
                MetricTags.OUTCOME, MetricTagSanitizer.sanitize(decision),
                MetricTags.APPROVAL_DECISION, MetricTagSanitizer.sanitize(decision)));
    }

    public void recordRagSearch(
            String retrievalMode, boolean fallbackUsed, MetricOutcome outcome, long startedAtNanos) {
        Tags tags = Tags.of(
                MetricTags.COMPONENT, RAG_COMPONENT,
                MetricTags.OPERATION, "search",
                MetricTags.OUTCOME, MetricTagSanitizer.sanitize(outcome),
                MetricTags.RETRIEVAL_MODE, MetricTagSanitizer.sanitize(retrievalMode),
                MetricTags.FALLBACK, MetricTagSanitizer.booleanTag(fallbackUsed));
        recordCounter(MetricNames.RAG_SEARCH_TOTAL, tags);
        recordTimer(MetricNames.RAG_SEARCH_DURATION, tags, startedAtNanos);
    }

    public void recordProviderCall(String providerType, String operation, MetricOutcome outcome, long startedAtNanos) {
        Tags tags = Tags.of(
                MetricTags.COMPONENT, PROVIDER_COMPONENT,
                MetricTags.OPERATION, MetricTagSanitizer.sanitize(operation),
                MetricTags.OUTCOME, MetricTagSanitizer.sanitize(outcome),
                MetricTags.PROVIDER_TYPE, MetricTagSanitizer.sanitize(providerType));
        recordCounter(MetricNames.PROVIDER_CALL_TOTAL, tags);
        recordTimer(MetricNames.PROVIDER_CALL_DURATION, tags, startedAtNanos);
    }

    private void recordCounter(String name, Tags tags) {
        try {
            Counter.builder(name).tags(tags).register(meterRegistry).increment();
        } catch (RuntimeException exception) {
            // Metrics must never change business runtime behavior.
        }
    }

    private void recordTimer(String name, Tags tags, long startedAtNanos) {
        try {
            Timer.builder(name)
                    .tags(tags)
                    .register(meterRegistry)
                    .record(Duration.ofNanos(elapsedNanos(startedAtNanos)));
        } catch (RuntimeException exception) {
            // Metrics must never change business runtime behavior.
        }
    }

    private static long elapsedNanos(long startedAtNanos) {
        return Math.max(0L, System.nanoTime() - startedAtNanos);
    }
}
