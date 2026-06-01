package com.example.aftersale.policy.rag.infrastructure.pgvector;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds opt-in PGvector settings for the explicit rag-postgres profile.
 */
@ConfigurationProperties(prefix = "agent.rag.vector-store.pgvector")
public record PgVectorProperties(
        boolean enabled,
        String jdbcUrl,
        String username,
        String password,
        String schema,
        boolean initializeSchema,
        int dimensions) {

    public void validate() {
        if (!enabled) {
            return;
        }
        if (isBlank(jdbcUrl) || isBlank(username) || isBlank(password)) {
            throw new PgVectorConfigurationException(
                    "PGvector profile requires AFTERSALE_PGVECTOR_URL, AFTERSALE_PGVECTOR_USERNAME, "
                            + "and AFTERSALE_PGVECTOR_PASSWORD when agent.rag.vector-store.pgvector.enabled=true");
        }
        if (dimensions <= 0) {
            throw new PgVectorConfigurationException("PGvector embedding dimensions must be greater than zero");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
