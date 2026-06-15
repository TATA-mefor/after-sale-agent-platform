package io.github.tatame.aftersale.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "agent.security")
public record SecurityProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("X-API-Key") String apiKeyHeader,
        @DefaultValue ApiKeys apiKeys) {

    public SecurityProperties {
        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            apiKeyHeader = "X-API-Key";
        }
        if (apiKeys == null) {
            apiKeys = new ApiKeys("", "", "", "");
        }
    }

    public record ApiKeys(
            @DefaultValue("") String admin,
            @DefaultValue("") String supervisor,
            @DefaultValue("") String operator,
            @DefaultValue("") String systemService) {
    }
}
