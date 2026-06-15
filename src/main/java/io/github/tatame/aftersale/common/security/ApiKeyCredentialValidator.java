package io.github.tatame.aftersale.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class ApiKeyCredentialValidator {

    private final Map<SecurityRole, byte[]> apiKeysByRole;

    private ApiKeyCredentialValidator(Map<SecurityRole, byte[]> apiKeysByRole) {
        this.apiKeysByRole = copyApiKeys(apiKeysByRole);
    }

    public static ApiKeyCredentialValidator from(SecurityProperties properties) {
        Map<SecurityRole, byte[]> configuredKeys = new EnumMap<>(SecurityRole.class);
        SecurityProperties.ApiKeys apiKeys = properties.apiKeys();
        addIfPresent(configuredKeys, SecurityRole.ADMIN, apiKeys.admin());
        addIfPresent(configuredKeys, SecurityRole.SUPERVISOR, apiKeys.supervisor());
        addIfPresent(configuredKeys, SecurityRole.AGENT_OPERATOR, apiKeys.operator());
        addIfPresent(configuredKeys, SecurityRole.SYSTEM_SERVICE, apiKeys.systemService());

        if (properties.enabled() && configuredKeys.isEmpty()) {
            throw new IllegalStateException("API key security is enabled but no API keys are configured.");
        }
        return new ApiKeyCredentialValidator(configuredKeys);
    }

    public Optional<ApiKeyPrincipal> authenticate(String candidateApiKey) {
        if (isBlank(candidateApiKey)) {
            return Optional.empty();
        }

        byte[] candidate = candidateApiKey.getBytes(StandardCharsets.UTF_8);
        for (Map.Entry<SecurityRole, byte[]> entry : apiKeysByRole.entrySet()) {
            if (MessageDigest.isEqual(candidate, entry.getValue())) {
                return Optional.of(new ApiKeyPrincipal(entry.getKey()));
            }
        }
        return Optional.empty();
    }

    public boolean hasConfiguredKeys() {
        return !apiKeysByRole.isEmpty();
    }

    private static void addIfPresent(
            Map<SecurityRole, byte[]> configuredKeys,
            SecurityRole role,
            String apiKey) {
        if (!isBlank(apiKey)) {
            configuredKeys.put(role, apiKey.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static Map<SecurityRole, byte[]> copyApiKeys(Map<SecurityRole, byte[]> source) {
        Map<SecurityRole, byte[]> copy = new EnumMap<>(SecurityRole.class);
        for (Map.Entry<SecurityRole, byte[]> entry : source.entrySet()) {
            copy.put(entry.getKey(), Arrays.copyOf(entry.getValue(), entry.getValue().length));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
