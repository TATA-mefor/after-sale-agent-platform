package io.github.tatame.aftersale.common.observability.metrics;

/**
 * Shared outcome vocabulary for application metrics.
 */
public enum MetricOutcome {
    APPROVED,
    FAILED,
    REJECTED,
    REQUESTED,
    REQUIRES_APPROVAL,
    STARTED,
    SUCCEEDED,
    UNKNOWN
}
