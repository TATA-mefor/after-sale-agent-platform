package io.github.tatame.aftersale.policy.rag.application;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic embedding client for offline tests.
 */
public class FakeEmbeddingClient implements EmbeddingClient {

    public static final int DEFAULT_DIMENSION = 8;

    private final int dimension;

    public FakeEmbeddingClient() {
        this(DEFAULT_DIMENSION);
    }

    public FakeEmbeddingClient(int dimension) {
        this.dimension = Math.max(1, dimension);
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        List<Double> vector = new ArrayList<>(dimension);
        int seed = request.text().hashCode();
        for (int index = 0; index < dimension; index++) {
            int bucket = Math.floorMod(seed + (index * 31), 2000);
            vector.add((bucket - 1000) / 1000.0d);
        }
        return new EmbeddingResponse(
                request.model(),
                dimension,
                vector,
                estimateTokens(request.text()));
    }

    private static int estimateTokens(String text) {
        return Math.max(1, text.length() / 4);
    }
}
