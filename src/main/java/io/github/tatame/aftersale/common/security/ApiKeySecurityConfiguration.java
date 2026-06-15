package io.github.tatame.aftersale.common.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class ApiKeySecurityConfiguration {

    private static final String[] HEALTH_ENDPOINTS = {
            "/actuator/health",
            "/actuator/health/liveness",
            "/actuator/health/readiness"
    };

    private static final String[] OPENAPI_ENDPOINTS = {
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**"
    };

    private static final String[] AGENT_OPERATOR_AUTHORITIES = {
            "ROLE_ADMIN",
            "ROLE_SUPERVISOR",
            "ROLE_AGENT_OPERATOR"
    };

    private static final String[] APPLICATION_AUTHORITIES = {
            "ROLE_ADMIN",
            "ROLE_SUPERVISOR",
            "ROLE_AGENT_OPERATOR",
            "ROLE_SYSTEM_SERVICE"
    };

    @Bean
    ApiKeyCredentialValidator apiKeyCredentialValidator(SecurityProperties properties) {
        return ApiKeyCredentialValidator.from(properties);
    }

    @Bean
    SecurityFilterChain apiKeySecurityFilterChain(
            HttpSecurity http,
            SecurityProperties properties,
            ApiKeyCredentialValidator credentialValidator) throws Exception {
        configureStatelessApiDefaults(http);

        if (!properties.enabled()) {
            http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
            return http.build();
        }

        http.addFilterBefore(
                new ApiKeyAuthenticationFilter(properties.apiKeyHeader(), credentialValidator),
                UsernamePasswordAuthenticationFilter.class);
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HEALTH_ENDPOINTS).permitAll()
                .requestMatchers(OPENAPI_ENDPOINTS).hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPERVISOR")
                .requestMatchers("/actuator/prometheus").hasAnyAuthority("ROLE_ADMIN", "ROLE_SYSTEM_SERVICE")
                .requestMatchers(HttpMethod.POST, "/api/approval-requests/*/approve")
                .hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPERVISOR")
                .requestMatchers(HttpMethod.POST, "/api/approval-requests/*/reject")
                .hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPERVISOR")
                .requestMatchers(HttpMethod.GET, "/api/approval-requests/**")
                .hasAnyAuthority(AGENT_OPERATOR_AUTHORITIES)
                .requestMatchers(HttpMethod.GET, "/api/agent-runs/*/traces")
                .hasAnyAuthority(AGENT_OPERATOR_AUTHORITIES)
                .requestMatchers(HttpMethod.GET, "/api/agent-runs/*/execution-tree")
                .hasAnyAuthority(AGENT_OPERATOR_AUTHORITIES)
                .requestMatchers("/api/tickets/**", "/api/agent-runs/**", "/api/health")
                .hasAnyAuthority(APPLICATION_AUTHORITIES)
                .anyRequest().authenticated());
        return http.build();
    }

    private static void configureStatelessApiDefaults(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN")));
    }

    private static void writeError(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "ApiKey");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"Access denied.\"}");
    }
}
