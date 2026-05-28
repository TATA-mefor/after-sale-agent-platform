package com.example.aftersale.policy.rag.health;

import java.net.URI;
import java.net.URISyntaxException;

final class RagHealthDetailSanitizer {

    private static final int MAX_DETAIL_LENGTH = 120;

    private RagHealthDetailSanitizer() {
    }

    static String provider(String provider) {
        if (isBlank(provider)) {
            return "none";
        }
        return sanitize(provider.trim().toLowerCase());
    }

    static String safeEndpointHost(String host) {
        if (isBlank(host)) {
            return "not-configured";
        }
        return sanitize(host.trim());
    }

    static String safeJdbcLocation(String jdbcUrl) {
        if (isBlank(jdbcUrl)) {
            return "not-configured";
        }
        String normalized = jdbcUrl.trim();
        if (!normalized.startsWith("jdbc:postgresql:")) {
            return "configured";
        }
        try {
            URI uri = new URI(normalized.substring("jdbc:".length()));
            String host = uri.getHost();
            String path = uri.getPath();
            String database = path == null || path.length() <= 1 ? "unknown" : path.substring(1);
            if (isBlank(host)) {
                return "postgresql://configured";
            }
            return "postgresql://" + host + "/" + sanitize(database);
        } catch (URISyntaxException exception) {
            return "postgresql://configured";
        }
    }

    static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = value
                .replaceAll("(?i)(api[_-]?key|password|token)=([^\\s,;]+)", "$1=***")
                .replaceAll("(?i)(api[_-]?key|password|token):([^\\s,;]+)", "$1:***")
                .replaceAll("(?i)Bearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer ***")
                .replaceAll("sk-[A-Za-z0-9_-]+", "sk-***")
                .replaceAll("[A-Za-z]:\\\\[^\\s,;]+", "[local-path]")
                .replaceAll("/Users/[^\\s,;]+", "[local-path]")
                .replaceAll("(?i)//([^/@\\s]+):([^/@\\s]+)@", "//***:***@");
        if (sanitized.length() > MAX_DETAIL_LENGTH) {
            return sanitized.substring(0, MAX_DETAIL_LENGTH);
        }
        return sanitized;
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
