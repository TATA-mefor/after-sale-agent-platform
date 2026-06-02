package io.github.tatame.aftersale.policy.rag.infrastructure.pgvector;

/**
 * Sanitized exception for opt-in PGvector repository failures.
 */
public class PgVectorRepositoryException extends RuntimeException {

    public PgVectorRepositoryException(String message) {
        super(message);
    }
}
