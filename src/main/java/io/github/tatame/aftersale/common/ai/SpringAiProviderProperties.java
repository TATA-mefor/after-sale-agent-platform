package io.github.tatame.aftersale.common.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring AI provider opt-in flags shared by chat and embedding adapters.
 */
@ConfigurationProperties(prefix = "agent.spring-ai")
public class SpringAiProviderProperties {

    private boolean enabled;

    private boolean chatEnabled;

    private boolean embeddingEnabled;

    private String providerType = "openai";

    private String apiKey = "";

    private String endpointHost = "spring-ai-managed";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isChatEnabled() {
        return chatEnabled;
    }

    public void setChatEnabled(boolean chatEnabled) {
        this.chatEnabled = chatEnabled;
    }

    public boolean isEmbeddingEnabled() {
        return embeddingEnabled;
    }

    public void setEmbeddingEnabled(boolean embeddingEnabled) {
        this.embeddingEnabled = embeddingEnabled;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getEndpointHost() {
        return endpointHost;
    }

    public void setEndpointHost(String endpointHost) {
        this.endpointHost = endpointHost;
    }
}
