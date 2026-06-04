package io.github.tatame.aftersale.common.api;

import io.github.tatame.aftersale.common.observability.correlation.CorrelationIdFilter;

/**
 * Backward-compatible alias for the V3 request-id filter type.
 *
 * <p>Spring registers {@link CorrelationIdFilter}; this type remains for legacy tests and references.
 */
public class RequestIdFilter extends CorrelationIdFilter {
}
