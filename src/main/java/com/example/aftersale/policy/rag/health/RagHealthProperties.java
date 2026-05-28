package com.example.aftersale.policy.rag.health;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Offline-safe RAG health indicator settings.
 */
@ConfigurationProperties(prefix = "agent.rag.health")
public class RagHealthProperties {

    private boolean enabled = true;

    private boolean includeDetails;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIncludeDetails() {
        return includeDetails;
    }

    public void setIncludeDetails(boolean includeDetails) {
        this.includeDetails = includeDetails;
    }
}
