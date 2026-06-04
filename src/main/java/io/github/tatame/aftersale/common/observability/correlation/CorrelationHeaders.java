package io.github.tatame.aftersale.common.observability.correlation;

/**
 * HTTP header and MDC keys used for local request correlation.
 */
public final class CorrelationHeaders {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    private CorrelationHeaders() {
    }
}
