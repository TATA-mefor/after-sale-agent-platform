package com.example.aftersale.common.ai;

/**
 * Creates bounded, credential-safe Spring AI provider error summaries.
 */
public final class SpringAiProviderErrorFormatter {

    private static final int MAX_MESSAGE_LENGTH = 500;

    private SpringAiProviderErrorFormatter() {
    }

    public static String format(
            String provider,
            String model,
            SpringAiProviderProperties properties,
            Throwable exception) {
        return "Spring AI provider call failed"
                + " provider=" + provider
                + " providerType=" + safe(properties.getProviderType())
                + " endpointHost=" + safe(properties.getEndpointHost())
                + " model=" + safe(model)
                + " errorClass=" + exception.getClass().getSimpleName()
                + " message=" + sanitize(exception.getMessage(), properties.getApiKey());
    }

    public static String configuration(String message, SpringAiProviderProperties properties) {
        return "Spring AI provider configuration error"
                + " provider=spring-ai"
                + " providerType=" + safe(properties.getProviderType())
                + " endpointHost=" + safe(properties.getEndpointHost())
                + " message=" + sanitize(message, properties.getApiKey());
    }

    public static String sanitize(String value, String apiKey) {
        String sanitized = value == null ? "" : value;
        if (apiKey != null && !apiKey.isBlank()) {
            sanitized = sanitized.replace(apiKey, "***");
        }
        sanitized = sanitized.replaceAll("sk-[A-Za-z0-9_-]+", "sk-***");
        sanitized = sanitized.replaceAll("Bearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer ***");
        if (sanitized.length() > MAX_MESSAGE_LENGTH) {
            return sanitized.substring(0, MAX_MESSAGE_LENGTH);
        }
        return sanitized;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
