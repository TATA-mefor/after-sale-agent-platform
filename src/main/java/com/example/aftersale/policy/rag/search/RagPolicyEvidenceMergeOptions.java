package com.example.aftersale.policy.rag.search;

public record RagPolicyEvidenceMergeOptions(
        int topK,
        double minScore,
        double keywordWeight,
        double vectorWeight,
        boolean preferKeywordWhenTie,
        boolean dedupByChunkId,
        boolean dedupByPolicyId,
        boolean dedupBySnippet,
        boolean includeKeywordOnly,
        boolean includeVectorOnly) {

    public static final int DEFAULT_TOP_K = 5;
    public static final int MAX_TOP_K = 20;
    public static final double DEFAULT_KEYWORD_WEIGHT = 0.45d;
    public static final double DEFAULT_VECTOR_WEIGHT = 0.55d;

    public RagPolicyEvidenceMergeOptions {
        if (topK < 1 || topK > MAX_TOP_K) {
            throw new IllegalArgumentException("topK must be between 1 and " + MAX_TOP_K);
        }
        if (!Double.isFinite(minScore) || minScore < 0.0d || minScore > 1.0d) {
            throw new IllegalArgumentException("minScore must be between 0.0 and 1.0");
        }
        if (!Double.isFinite(keywordWeight) || keywordWeight < 0.0d) {
            throw new IllegalArgumentException("keywordWeight must be non-negative");
        }
        if (!Double.isFinite(vectorWeight) || vectorWeight < 0.0d) {
            throw new IllegalArgumentException("vectorWeight must be non-negative");
        }
        if (keywordWeight + vectorWeight == 0.0d) {
            throw new IllegalArgumentException("keywordWeight and vectorWeight must not both be zero");
        }
    }

    public static RagPolicyEvidenceMergeOptions defaults() {
        return new RagPolicyEvidenceMergeOptions(
                DEFAULT_TOP_K,
                0.0d,
                DEFAULT_KEYWORD_WEIGHT,
                DEFAULT_VECTOR_WEIGHT,
                true,
                true,
                true,
                true,
                true,
                true);
    }
}
