package io.github.tatame.aftersale.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final String apiKeyHeader;

    private final ApiKeyCredentialValidator credentialValidator;

    public ApiKeyAuthenticationFilter(String apiKeyHeader, ApiKeyCredentialValidator credentialValidator) {
        this.apiKeyHeader = apiKeyHeader;
        this.credentialValidator = credentialValidator;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(apiKeyHeader);
        if (apiKey == null || apiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        var principal = credentialValidator.authenticate(apiKey);
        if (principal.isEmpty()) {
            writeUnauthorized(response);
            return;
        }
        authenticate(principal.get(), filterChain, request, response);
    }

    private static void authenticate(
            ApiKeyPrincipal principal,
            FilterChain filterChain,
            HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        try {
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(
                    principal.role().authority()));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "ApiKey");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid API key.\"}");
    }
}
