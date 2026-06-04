package io.github.tatame.aftersale.common.observability.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds local correlation identifiers to HTTP responses and request-scoped MDC.
 *
 * <p>This filter never reads request body, query parameters, Authorization, or Cookie headers.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = CorrelationIds.safeOrGenerated(
                request.getHeader(CorrelationHeaders.CORRELATION_ID_HEADER));
        String requestId = CorrelationIds.safeOrGenerated(request.getHeader(CorrelationHeaders.REQUEST_ID_HEADER));

        response.setHeader(CorrelationHeaders.CORRELATION_ID_HEADER, correlationId);
        response.setHeader(CorrelationHeaders.REQUEST_ID_HEADER, requestId);
        MDC.put(CorrelationHeaders.CORRELATION_ID_MDC_KEY, correlationId);
        MDC.put(CorrelationHeaders.REQUEST_ID_MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationHeaders.CORRELATION_ID_MDC_KEY);
            MDC.remove(CorrelationHeaders.REQUEST_ID_MDC_KEY);
        }
    }
}
