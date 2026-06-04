package io.github.tatame.aftersale.common.observability.metrics;

/**
 * Project-owned Micrometer meter names.
 */
public final class MetricNames {

    public static final String AGENT_RUN_TOTAL = "aftersale.agent.run.total";
    public static final String AGENT_RUN_DURATION = "aftersale.agent.run.duration";
    public static final String TOOL_CALL_TOTAL = "aftersale.tool.call.total";
    public static final String TOOL_CALL_DURATION = "aftersale.tool.call.duration";
    public static final String APPROVAL_REQUEST_TOTAL = "aftersale.approval.request.total";
    public static final String APPROVAL_DECISION_TOTAL = "aftersale.approval.decision.total";
    public static final String RAG_SEARCH_TOTAL = "aftersale.rag.search.total";
    public static final String RAG_SEARCH_DURATION = "aftersale.rag.search.duration";
    public static final String PROVIDER_CALL_TOTAL = "aftersale.provider.call.total";
    public static final String PROVIDER_CALL_DURATION = "aftersale.provider.call.duration";

    private MetricNames() {
    }
}
