package io.github.tatame.aftersale.policy.rag.infrastructure.pgvector;

/**
 * Signals an invalid opt-in PGvector profile configuration without exposing credentials.
 */
public class PgVectorConfigurationException extends RuntimeException {

    public PgVectorConfigurationException(String message) {
        super(message);
    }
}
