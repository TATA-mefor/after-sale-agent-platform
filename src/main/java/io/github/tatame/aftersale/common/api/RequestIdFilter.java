package io.github.tatame.aftersale.common.api;

import io.github.tatame.aftersale.common.observability.ObservabilityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 为每个 HTTP 请求建立 requestId 响应头和 MDC 上下文。
 *
 * <p>边界：该过滤器只处理观测性关联标识，不改变认证、业务权限或 Agent 执行语义。
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = requestId(request);
        response.setHeader(ObservabilityConstants.REQUEST_ID_HEADER, requestId);
        MDC.put(ObservabilityConstants.REQUEST_ID, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(ObservabilityConstants.REQUEST_ID);
        }
    }

    private static String requestId(HttpServletRequest request) {
        String requestId = request.getHeader(ObservabilityConstants.REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId;
    }
}
