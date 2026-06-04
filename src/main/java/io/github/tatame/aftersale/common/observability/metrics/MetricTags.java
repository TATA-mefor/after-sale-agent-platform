package io.github.tatame.aftersale.common.observability.metrics;

/**
 * Low-cardinality metric tag keys.
 */
public final class MetricTags {

    public static final String APPROVAL_DECISION = "approval_decision";
    public static final String COMPONENT = "component";
    public static final String FALLBACK = "fallback";
    public static final String OPERATION = "operation";
    public static final String OUTCOME = "outcome";
    public static final String PROVIDER_TYPE = "provider_type";
    public static final String RETRIEVAL_MODE = "retrieval_mode";
    public static final String RISK_LEVEL = "risk_level";
    public static final String STATUS = "status";
    public static final String TOOL_NAME = "tool_name";

    private MetricTags() {
    }
}
