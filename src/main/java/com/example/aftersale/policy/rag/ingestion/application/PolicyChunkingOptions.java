package com.example.aftersale.policy.rag.ingestion.application;

public record PolicyChunkingOptions(
        int maxChunkChars,
        int overlapChars,
        int maxChunksPerDocument,
        int tokenEstimateDivisor,
        boolean preserveParagraphBoundary,
        int minChunkChars,
        PolicyChunkingStrategy strategy) {

    private static final int DEFAULT_MAX_CHUNK_CHARS = 800;
    private static final int DEFAULT_OVERLAP_CHARS = 100;
    private static final int DEFAULT_MAX_CHUNKS_PER_DOCUMENT = 100;
    private static final int DEFAULT_TOKEN_ESTIMATE_DIVISOR = 4;
    private static final int DEFAULT_MIN_CHUNK_CHARS = 120;

    public PolicyChunkingOptions {
        if (maxChunkChars <= 0) {
            throw new IllegalArgumentException("maxChunkChars must be positive");
        }
        if (overlapChars < 0) {
            throw new IllegalArgumentException("overlapChars must be greater than or equal to zero");
        }
        if (overlapChars >= maxChunkChars) {
            throw new IllegalArgumentException("overlapChars must be less than maxChunkChars");
        }
        if (maxChunksPerDocument <= 0) {
            throw new IllegalArgumentException("maxChunksPerDocument must be positive");
        }
        if (tokenEstimateDivisor <= 0) {
            throw new IllegalArgumentException("tokenEstimateDivisor must be positive");
        }
        if (minChunkChars <= 0) {
            throw new IllegalArgumentException("minChunkChars must be positive");
        }
        if (minChunkChars > maxChunkChars) {
            throw new IllegalArgumentException("minChunkChars must be less than or equal to maxChunkChars");
        }
        if (strategy == null) {
            strategy = PolicyChunkingStrategy.DETERMINISTIC_CHARACTER_WINDOW;
        }
    }

    public static PolicyChunkingOptions defaults() {
        return new PolicyChunkingOptions(
                DEFAULT_MAX_CHUNK_CHARS,
                DEFAULT_OVERLAP_CHARS,
                DEFAULT_MAX_CHUNKS_PER_DOCUMENT,
                DEFAULT_TOKEN_ESTIMATE_DIVISOR,
                true,
                DEFAULT_MIN_CHUNK_CHARS,
                PolicyChunkingStrategy.DETERMINISTIC_CHARACTER_WINDOW);
    }
}
