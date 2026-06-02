package io.github.tatame.aftersale.policy.rag.infrastructure.pgvector;

/**
 * Sanitized PGvector profile readiness marker.
 */
public record PgVectorProfileGuard(
        boolean enabled,
        String jdbcUrl,
        String username,
        String schema,
        boolean initializeSchema,
        int dimensions) {
}
