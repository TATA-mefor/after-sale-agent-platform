package io.github.tatame.aftersale.agent.infrastructure.llm;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 生成脱敏后的 Provider 错误摘要。
 *
 * <p>边界：错误摘要用于 AgentPlanValidationException 和日志排障，必须截断响应体并移除凭证类内容。
 */
final class LlmProviderErrorFormatter {

    private static final int MAX_BODY_LENGTH = 500;

    private LlmProviderErrorFormatter() {
    }

    static String format(LlmProviderSettings settings, int statusCode, String responseBody) {
        return "LLM provider returned HTTP " + statusCode
                + " provider=" + settings.provider().configValue()
                + " endpointHost=" + endpointHost(settings.endpoint())
                + " model=" + settings.model()
                + " body=" + sanitize(responseBody, settings.apiKey());
    }

    static String formatCallFailure(LlmProviderSettings settings, String message) {
        return "LLM provider call failed"
                + " provider=" + settings.provider().configValue()
                + " endpointHost=" + endpointHost(settings.endpoint())
                + " model=" + settings.model()
                + " message=" + sanitize(message, settings.apiKey());
    }

    static String sanitize(String value, String apiKey) {
        String sanitized = value == null ? "" : value;
        if (apiKey != null && !apiKey.isBlank()) {
            sanitized = sanitized.replace(apiKey, "***");
        }
        sanitized = sanitized.replaceAll("sk-[A-Za-z0-9_-]+", "sk-***");
        sanitized = sanitized.replaceAll("Bearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer ***");
        if (sanitized.length() > MAX_BODY_LENGTH) {
            return sanitized.substring(0, MAX_BODY_LENGTH);
        }
        return sanitized;
    }

    private static String endpointHost(String endpoint) {
        try {
            URI uri = new URI(endpoint);
            String host = uri.getHost();
            return host == null || host.isBlank() ? "unknown" : host;
        } catch (URISyntaxException exception) {
            return "invalid-endpoint";
        }
    }
}
